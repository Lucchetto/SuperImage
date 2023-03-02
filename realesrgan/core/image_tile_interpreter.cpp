//
// Created by Zhenxiang Chen on 06/02/23.
//

#include <thread>

#include "image_tile_interpreter.h"

ImageTileInterpreter::ImageTileInterpreter(
        const mnn_model* model,
        const image_dimensions tile_dimensions) : tile_dimensions(tile_dimensions) {
    MNN::ScheduleConfig config;
    MNN::BackendConfig backendConfig;
    backendConfig.memory = MNN::BackendConfig::Memory_High;
    backendConfig.power = MNN::BackendConfig::Power_High;
    backendConfig.precision = MNN::BackendConfig::Precision_Low;
    config.backendConfig = &backendConfig;
    config.type = MNN_FORWARD_VULKAN;
    config.backupType = MNN_FORWARD_OPENCL;
    config.numThread = std::thread::hardware_concurrency();

    interpreter = MNN::Interpreter::createFromBuffer(model->data, model->size);
    if (interpreter == nullptr) {
        throw ImageTileInterpreterException(ImageTileInterpreterException::Error::CreateInterpreterFailed);
    }

    session = interpreter->createSession(config);
    if (session == nullptr) {
        throw ImageTileInterpreterException(ImageTileInterpreterException::Error::CreateBackendFailed);
    }

    interpreter_input = interpreter->getSessionInput(session, nullptr);
    // We store matrix as row major so ignore MNN default tensor orientation
    interpreter->resizeTensor(
            interpreter_input,
            1,
            REALESRGAN_IMAGE_CHANNELS,
            tile_dimensions.width,
            tile_dimensions.height);
    interpreter->resizeSession(session);
    interpreter_output = interpreter->getSessionOutput(session, nullptr);

    input_tensor = new MNN::Tensor(interpreter_input, MNN::Tensor::CAFFE);
    output_tensor = new MNN::Tensor(interpreter_output, MNN::Tensor::CAFFE);

    input_buffer = input_tensor->host<float>();
    output_buffer = output_tensor->host<float>();
}

void ImageTileInterpreter::inference() const {
    // Feed data to the interpreter
    interpreter_input->copyFromHostTensor(input_tensor);

    // Run the interpreter
    interpreter->runSession(session);

    // Extract result from interpreter
    interpreter_output->copyToHostTensor(output_tensor);
}

ImageTileInterpreter::~ImageTileInterpreter() {
    MNN::Tensor::destroy(input_tensor);
    MNN::Tensor::destroy(output_tensor);
    interpreter->releaseSession(session);
    MNN::Interpreter::destroy(interpreter);
}
