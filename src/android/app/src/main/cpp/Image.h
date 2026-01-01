#pragma once

struct Image
{
	sint32* m_colors = nullptr;
	int m_width = 0;
	int m_height = 0;
	int m_channels = 0;

	Image(Image&& image);

	Image(const std::vector<uint8>& imageBytes);

	bool isOk() const;

	~Image();
};
