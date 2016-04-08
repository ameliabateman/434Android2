An Android project run via AndroidManifest.xml
This is an Android photo doodler app which allows users to download a photo or upload a photo of their own and then draw an impressionist version of the photo on a seperate Canvas. There are 3 brush types: circle, square, and speed circle. For the former two, the shape of the brush differs, but each takes the color from the corresponding pixel in the picture and creates a larger version of it in the right pane. The speed circle brush determines brush radius as a function of velocity.
An additional feature allows various types of symmetry in the drawing canvas.

BasicImageDownloader.java downloads a set list of images from the internet and adds them to the image gallery, which is useful when using an emulator.
ImpressionistView.java as all of the interesting parts of the code.
It includes a bitmap for the left panel and a canvas with a paint object for the right. Event handling, such as clearing the canvas and changing settings, all happens here. onTouchEvent, the location of the touch is recorded and the drawing is contained within switch statements on brush type and symmetry. 

Sources:
Jon's sample code
ELMS discussion boards
Various stackoverflow posts about saving canvases as bitmaps, but most of it ended up being from ELMS
