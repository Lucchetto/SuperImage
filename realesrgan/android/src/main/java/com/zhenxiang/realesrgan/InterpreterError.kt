package com.zhenxiang.realesrgan

enum class InterpreterError {

    UNKNOWN,

    /**
     * MNN model failed to load
     */
    CREATE_INTERPRETER,

    /**
     * Required MNN backends not available or invalid config
     */
    CREATE_SESSION;

    companion object {

        /**
         * Convert from ImageTileInterpreterException::Error value to enum
         */
        fun fromNativeErrorEnum(value: Int?) = when(value) {
            1 -> CREATE_INTERPRETER
            2 -> CREATE_SESSION
            else -> UNKNOWN
        }
    }
}