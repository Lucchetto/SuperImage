# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.18.1)

# Declares and names the project.
project("realesrgan")

set(EIGEN_INCLUDE "${CMAKE_CURRENT_SOURCE_DIR}/eigen")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++17")
set(CMAKE_CXX_STANDARD 17)

set(
        REALERSGAN_SOURCES

        jni_common/coroutine_utils.cpp
        jni_common/mnn_model.cpp
        jni_common/progress_tracker.cpp
        image_tile_interpreter.cpp
        upscaling.cpp
)

if(ANDROID)
    # Reproducible builds
    add_link_options("LINKER:--hash-style=gnu,--build-id=none")

    list(APPEND REALERSGAN_SOURCES android/realesrgan_jni.cpp android/bitmap_utils.cpp)

    find_library(jnigraphics-lib jnigraphics)
else()
    list(APPEND REALERSGAN_SOURCES desktop/realesrgan_jni.cpp desktop/image_utils.cpp)

    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})
endif()

option(MNN_VULKAN "" ON)
option(MNN_OPENCL "" ON)

add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/MNN)
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/MNN/include)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        realesrgan

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ${REALERSGAN_SOURCES})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

include_directories(${EIGEN_INCLUDE})

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        realesrgan

        ${jnigraphics-lib}
        MNN)