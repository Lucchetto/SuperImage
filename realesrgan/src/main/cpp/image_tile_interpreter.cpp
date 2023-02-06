//
// Created by Zhenxiang Chen on 06/02/23.
//

#include <thread>

#include "image_tile_interpreter.h"

ImageTileInterpreter::ImageTileInterpreter(const mnn_model* model, const int tile_size) : tile_size(tile_size) {
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
        throw std::runtime_error("Failed to create MNN interpreter");
    }
    session = interpreter->createSession(config);

    interpreter_input = interpreter->getSessionInput(session, nullptr);
    interpreter->resizeTensor(interpreter_input, 1, REALESRGAN_IMAGE_CHANNELS, tile_size, tile_size);
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
    delete input_tensor;
    delete output_tensor;
    interpreter->releaseSession(session);
    delete interpreter;
}
