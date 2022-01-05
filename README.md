# Object Detection App For Visually Impaired Users

## Descriptions
This app made for visually impaired users. I believe that this app is going to make the life of blind and visually impaired users easier. App is quite intuitive and very easy to use. It detects object in image taken by user and gives output as audio message, ex: *"This is the wallet"*

## Implementation
For the implementation of this app Camera 2.0 API, TextToSpeech API and [Pretrained Inception model](https://www.tensorflow.org/lite/guide/hosted_models) are used.  
First image is taken, then it's center is cropped as square and resolution changed to 299x299 pixels. After that image inputted to Inception model. Lastly, according to result audio message is otputted.  

## APK File
[Here is a link](https://drive.google.com/file/d/1kpsvyT1a3TWPt4yZkfBX1aOK5jbAPOvd/view?usp=sharing)

## Demo Video
[Video](https://www.youtube.com/watch?v=oz6v_al72ko&feature=youtu.be)

## Demo GIF
![](assets/demo.gif)
