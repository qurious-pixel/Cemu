#pragma once

#include "GameTitleLoader.h"
#include "JNIUtils.h"
#include <android/bitmap.h>
// TODO: Refactor this:
class AndroidGameTitleLoadedCallback : public GameTitleLoadedCallback
{
	jmethodID m_onGameTitleLoadedMID;
	JNIUtils::Scopedjobject m_gameTitleLoadedCallbackObj;
	jmethodID m_gameConstructorMID;
	JNIUtils::Scopedjclass m_gamejclass{"info/cemu/cemu/nativeinterface/NativeGameTitles$Game"};
	jmethodID m_createBitmapMID;
	JNIUtils::Scopedjclass m_bitmapClass{"android/graphics/Bitmap"};
	JNIUtils::Scopedjobject m_bitmapFormat;

  public:
	AndroidGameTitleLoadedCallback(jmethodID onGameTitleLoadedMID, jobject gameTitleLoadedCallbackObj)
		: m_onGameTitleLoadedMID(onGameTitleLoadedMID),
		  m_gameTitleLoadedCallbackObj(gameTitleLoadedCallbackObj)
	{
		JNIUtils::ScopedJNIENV env;
		m_bitmapFormat = JNIUtils::getEnumValue(env, "android/graphics/Bitmap$Config", "ARGB_8888");
		m_gameConstructorMID = env->GetMethodID(*m_gamejclass, "<init>", "(JLjava/lang/String;Ljava/lang/String;SSISSSIZLandroid/graphics/Bitmap;)V");
		m_createBitmapMID = env->GetStaticMethodID(*m_bitmapClass, "createBitmap", "([IIILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
	}

	void onTitleLoaded(const Game& game, const std::shared_ptr<Image>& icon) override
	{
		static JNIUtils::ScopedJNIENV env;
		jstring name = JNIUtils::toJString(env, game.name);
		jstring path = game.path.has_value() ? JNIUtils::toJString(env, game.path.value()) : nullptr;
		jobject bitmap = nullptr;
		sint32 lastPlayedYear = 0, lastPlayedMonth = 0, lastPlayedDay = 0;
		if (game.lastPlayed.has_value())
		{
			lastPlayedYear = static_cast<int>(game.lastPlayed->year());
			lastPlayedMonth = static_cast<unsigned int>(game.lastPlayed->month());
			lastPlayedDay = static_cast<unsigned int>(game.lastPlayed->day());
		}
		if (icon)
		{
			jintArray jIconData = env->NewIntArray(icon->m_width * icon->m_height);
			env->SetIntArrayRegion(jIconData, 0, icon->m_width * icon->m_height, icon->m_colors);
			bitmap = env->CallStaticObjectMethod(*m_bitmapClass, m_createBitmapMID, jIconData, icon->m_width, icon->m_height, *m_bitmapFormat);
			env->DeleteLocalRef(jIconData);
		}
		jobject gamejobject = env->NewObject(
			*m_gamejclass,
			m_gameConstructorMID,
			game.titleId,
			path,
			name,
			game.version,
			game.dlc,
			static_cast<sint32>(game.region),
			lastPlayedYear,
			lastPlayedMonth,
			lastPlayedDay,
			game.minutesPlayed,
			game.isFavorite,
			bitmap);
		env->CallVoidMethod(*m_gameTitleLoadedCallbackObj, m_onGameTitleLoadedMID, gamejobject);
		env->DeleteLocalRef(gamejobject);
		if (bitmap != nullptr)
			env->DeleteLocalRef(bitmap);
		if (path != nullptr)
			env->DeleteLocalRef(path);
		env->DeleteLocalRef(name);
	}
};
