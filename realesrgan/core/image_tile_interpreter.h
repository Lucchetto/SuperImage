//
// Created by Zhenxiang Chen on 06/02/23.
//

#ifndef SUPERIMAGE_IMAGE_TILE_INTERPRETER_H
#define SUPERIMAGE_IMAGE_TILE_INTERPRETER_H

#include "MNN/Interpreter.hpp"

#include "mnn_model.h"

#define REALESRGAN_IMAGE_CHANNELS 3


class ImageTileInterpreterException : public std::exception {

public:

    enum Error {
        CreateInterpreterFailed = 1,
        CreateBackendFailed = 2
    };

    const Error error;

    ImageTileInterpreterException(Error error) : error(error) {}

    const char* what() const noexcept override {
        switch (error) {
            case Error::CreateInterpreterFailed:
                return "Failed to create MNN interpreter";
            case Error::CreateBackendFailed:
                return "Failed to create MNN backend";
        }
    }
};

class ImageTileInterpreter {

public:
    ImageTileInterpreter(const mnn_model* model, const int tile_size);
    ~ImageTileInterpreter();

    const int tile_size;

    float* input_buffer;
    float* output_buffer;

    void inference() const;

private:
    MNN::Interpreter* interpreter;
    MNN::Session* session;
    MNN::Tensor* interpreter_input;
    MNN::Tensor* interpreter_output;
    MNN::Tensor* input_tensor;
    MNN::Tensor* output_tensor;
};

#endif //SUPERIMAGE_IMAGE_TILE_INTERPRETER_H
