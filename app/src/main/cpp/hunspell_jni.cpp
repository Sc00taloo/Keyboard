#include <jni.h>
#include <string>
#include <vector>
#include "hunspell/hunspell.hxx"

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_keyboard_spellChecker_SpellChecker_initHunspell(JNIEnv* env, jobject, jstring dicPath, jstring affPath) {
    const char* dic = env->GetStringUTFChars(dicPath, nullptr);
    const char* aff = env->GetStringUTFChars(affPath, nullptr);
    Hunspell* hunspell = new Hunspell(aff, dic);
    env->ReleaseStringUTFChars(dicPath, dic);
    env->ReleaseStringUTFChars(affPath, aff);
    return reinterpret_cast<jlong>(hunspell);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_keyboard_spellChecker_SpellChecker_destroyHunspell(JNIEnv*, jobject, jlong handle) {
    delete reinterpret_cast<Hunspell*>(handle);
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_example_keyboard_spellChecker_SpellChecker_suggest(JNIEnv* env, jobject, jlong handle, jstring word) {
    Hunspell* hunspell = reinterpret_cast<Hunspell*>(handle);
    const char* wordStr = env->GetStringUTFChars(word, nullptr);
    char** suggestions;
    int count = hunspell->suggest(&suggestions, wordStr);
    jobjectArray result = env->NewObjectArray(count, env->FindClass("java/lang/String"), nullptr);
    for (int i = 0; i < count; ++i) {
        env->SetObjectArrayElement(result, i, env->NewStringUTF(suggestions[i]));
        free(suggestions[i]);
    }
    free(suggestions);
    env->ReleaseStringUTFChars(word, wordStr);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_keyboard_spellChecker_SpellChecker_spell(JNIEnv* env, jobject, jlong handle, jstring word) {
    Hunspell* hunspell = reinterpret_cast<Hunspell*>(handle);
    const char* wordStr = env->GetStringUTFChars(word, nullptr);
    int result = hunspell->spell(wordStr);
    env->ReleaseStringUTFChars(word, wordStr);
    return result != 0;
}