import 'package:camera/camera.dart';

class CameraService {
  CameraController? _controller;
  List<CameraDescription>? _cameras;
  int _selectedCameraIndex = 0;
  bool _isFlashOn = false;

  Future<void> initialize() async {
    _cameras = await availableCameras();
    if (_cameras != null && _cameras!.isNotEmpty) {
      await _initializeController(_cameras![0]);
    }
  }

  Future<void> _initializeController(
    CameraDescription cameraDescription,
  ) async {
    final previousController = _controller;
    final newController = CameraController(
      cameraDescription,
      ResolutionPreset.high,
      enableAudio: false,
    );

    await previousController?.dispose();

    if (newController.value.hasError) {
      print('Camera error ${newController.value.errorDescription}');
      return;
    }

    try {
      await newController.initialize();
      _controller = newController;
    } catch (e) {
      print('Error initializing camera: $e');
    }
  }

  CameraController? get controller => _controller;
  bool get isFlashOn => _isFlashOn;

  Future<void> switchCamera() async {
    if (_cameras == null || _cameras!.length < 2) return;

    _selectedCameraIndex = (_selectedCameraIndex + 1) % _cameras!.length;
    await _initializeController(_cameras![_selectedCameraIndex]);
  }

  Future<void> toggleFlash() async {
    if (_controller == null || !_controller!.value.isInitialized) return;

    _isFlashOn = !_isFlashOn;
    await _controller!.setFlashMode(
      _isFlashOn ? FlashMode.torch : FlashMode.off,
    );
  }

  Future<XFile?> takePicture() async {
    if (_controller == null || !_controller!.value.isInitialized) {
      return null;
    }
    if (_controller!.value.isTakingPicture) {
      return null;
    }
    try {
      return await _controller!.takePicture();
    } catch (e) {
      print('Error taking picture: $e');
      return null;
    }
  }

  void dispose() {
    _controller?.dispose();
  }
}
