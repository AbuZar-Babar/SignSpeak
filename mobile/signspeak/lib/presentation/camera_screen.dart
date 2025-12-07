import 'dart:async';
import 'dart:io';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:signspeak/services/camera_service.dart';
import 'package:signspeak/services/api_service.dart';

class CameraScreen extends StatefulWidget {
  const CameraScreen({super.key});

  @override
  State<CameraScreen> createState() => _CameraScreenState();
}

class _CameraScreenState extends State<CameraScreen> {
  final CameraService _cameraService = CameraService();
  final ApiService _apiService = ApiService();
  bool _isTranslating = false;
  String _prediction = "Ready to Translate";
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }

  Future<void> _initializeCamera() async {
    await _cameraService.initialize();
    if (mounted) {
      setState(() {});
    }
  }

  void _toggleTranslation() {
    setState(() {
      _isTranslating = !_isTranslating;
    });

    if (_isTranslating) {
      _timer = Timer.periodic(const Duration(seconds: 1), (timer) async {
        await _captureAndPredict();
      });
    } else {
      _timer?.cancel();
      setState(() {
        _prediction = "Translation Stopped";
      });
    }
  }

  Future<void> _captureAndPredict() async {
    XFile? file = await _cameraService.takePicture();
    if (file != null) {
      try {
        final result = await _apiService.predictSign(File(file.path));
        if (mounted) {
          setState(() {
            _prediction =
                "${result['label']} (${(result['confidence'] * 100).toStringAsFixed(1)}%)";
          });
        }
      } catch (e) {
        print("Prediction error: $e");
      }
    }
  }

  Future<void> _switchCamera() async {
    await _cameraService.switchCamera();
    if (mounted) setState(() {});
  }

  Future<void> _toggleFlash() async {
    await _cameraService.toggleFlash();
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _cameraService.dispose();
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_cameraService.controller == null ||
        !_cameraService.controller!.value.isInitialized) {
      return const Scaffold(
        backgroundColor: Colors.black,
        body: Center(child: CircularProgressIndicator(color: Colors.white)),
      );
    }

    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          // Camera Preview
          Center(child: CameraPreview(_cameraService.controller!)),

          // Top Bar (Back Button & Flash)
          SafeArea(
            child: Align(
              alignment: Alignment.topCenter,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 16,
                  vertical: 8,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.arrow_back, color: Colors.white),
                      onPressed: () => Navigator.pop(context),
                    ),
                    const Hero(
                      tag: 'Translate',
                      child: Icon(Icons.camera_alt, color: Colors.white),
                    ),
                    IconButton(
                      icon: Icon(
                        _cameraService.isFlashOn
                            ? Icons.flash_on
                            : Icons.flash_off,
                        color: Colors.white,
                      ),
                      onPressed: _toggleFlash,
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Bottom Sheet (Prediction & Controls)
          Align(
            alignment: Alignment.bottomCenter,
            child: Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.transparent,
                    Colors.black.withOpacity(0.8),
                    Colors.black,
                  ],
                ),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Prediction Text
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.white.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.white.withOpacity(0.2)),
                    ),
                    child: Text(
                      _prediction,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(height: 32),

                  // Controls
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      // Switch Camera
                      IconButton(
                        icon: const Icon(
                          Icons.cameraswitch,
                          color: Colors.white,
                          size: 32,
                        ),
                        onPressed: _switchCamera,
                      ),

                      // Record Button
                      GestureDetector(
                        onTap: _toggleTranslation,
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 300),
                          height: 80,
                          width: 80,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: _isTranslating ? Colors.red : Colors.white,
                            border: Border.all(color: Colors.white, width: 4),
                          ),
                          child: Center(
                            child: Icon(
                              _isTranslating ? Icons.stop : Icons.play_arrow,
                              color:
                                  _isTranslating ? Colors.white : Colors.black,
                              size: 40,
                            ),
                          ),
                        ),
                      ),

                      // Placeholder for symmetry (or Gallery later)
                      const SizedBox(width: 48),
                    ],
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
