# Text Recognition Android App

This Android application leverages Firebase Machine Learning (ML) Kit to recognize text from images. Built using Android Studio with Java, it provides a seamless experience for extracting text from photos taken with the camera or selected from the device gallery.

## Features

- **Text Recognition:** Uses Firebase ML Kit to detect and extract text from images.
- **Image Selection:** Choose images from the device's camera or gallery.
- **Image Cropping:** Crop images before processing for more accurate text recognition.

## Technologies Used

- **Java** (Primary programming language)
- **Android Studio** (IDE)
- **Firebase ML Kit** (Text recognition)

## Getting Started

### Prerequisites
- Android Studio installed
- Firebase account and project setup

### Setup Instructions
1. **Clone the repository:**
   ```
   git clone https://github.com/code-with-prabhat/Textrecogination.git
   ```
2. **Open in Android Studio:**
   - Open the project folder in Android Studio.
3. **Configure Firebase:**
   - Add your `google-services.json` file to the `app/` directory.
   - Ensure Firebase ML Kit is enabled in your Firebase console.
4. **Build and Run:**
   - Connect your Android device or use an emulator.
   - Click "Run" in Android Studio.

## Usage

1. Launch the app.
2. Select an image from the camera or gallery.
3. Crop the image as needed.
4. Tap the button to recognize text.
5. View and copy the extracted text.

## Project Structure

- `app/src/main/java/com/textrecogination/` - Main application source code
- `app/src/main/res/` - Resources (layouts, drawables, etc.)
- `app/build.gradle` - App-level Gradle configuration
- `google-services.json` - Firebase configuration file

## License

This project is licensed under the MIT License.

## Author

Developed by [code-with-prabhat](https://github.com/code-with-prabhat)
