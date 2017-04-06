#include <jni.h>
#include <string>
#include <stdio.h>

extern "C"
jstring
Java_com_hadroncfy_aircraft_SearchDeviceActivity_stringFromJNI(
        JNIEnv* env,
        jobject cela) {
    const char *hello = "Hello from C++";
    return (env)->NewStringUTF(hello);
}
