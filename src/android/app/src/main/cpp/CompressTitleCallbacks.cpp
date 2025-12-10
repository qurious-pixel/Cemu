#include "CompressTitleCallbacks.h"

CompressTitleCallbacks::CompressTitleCallbacks(jobject compressTitleCallbacks)
	: m_compressTitleCallbacks{compressTitleCallbacks}
{
	JNIUtils::ScopedJNIENV env;
	JNIUtils::Scopedjclass compressTitleCallbacksClass("info/cemu/cemu/nativeinterface/NativeGameTitles$TitleCompressCallbacks");
	m_onFinishedMID = env->GetMethodID(*compressTitleCallbacksClass, "onFinished", "()V");
	m_onErrorMID = env->GetMethodID(*compressTitleCallbacksClass, "onError", "()V");
}

void CompressTitleCallbacks::onFinished()
{
	JNIUtils::ScopedJNIENV()->CallVoidMethod(*m_compressTitleCallbacks, m_onFinishedMID);
}

void CompressTitleCallbacks::onError()
{
	JNIUtils::ScopedJNIENV()->CallVoidMethod(*m_compressTitleCallbacks, m_onErrorMID);
}
