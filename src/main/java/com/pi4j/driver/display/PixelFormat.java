package com.pi4j.driver.display;

public enum PixelFormat {
	RGB_444,	// 12-bit color format with 4 bits for each color channel (red, green, blue)
	RGB_565,	// 16-bit color format that uses 5 bits for red, 6 bits for green, and 5 bits for blue
	RGB_666,	// 18-bit color format with 6 bits for each color channel (red, green, blue)
	RGB_888		// 24-bit color format with 8 bits for each color channel (red, green, blue)
}
