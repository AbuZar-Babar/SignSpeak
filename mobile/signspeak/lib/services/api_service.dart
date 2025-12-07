import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

class ApiService {
  // Replace with actual backend URL (e.g., 10.0.2.2 for Android emulator)
  static const String baseUrl = 'http://10.0.2.2:8000/api/v1';

  Future<Map<String, dynamic>> predictSign(File imageFile) async {
    try {
      print("Sending request to $baseUrl/inference/predict");
      var request = http.MultipartRequest(
        'POST',
        Uri.parse('$baseUrl/inference/predict'),
      );
      request.files.add(
        await http.MultipartFile.fromPath('file', imageFile.path),
      );

      var response = await request.send();
      var responseData = await response.stream.bytesToString();

      if (response.statusCode == 200) {
        return json.decode(responseData);
      } else {
        print("Backend error: ${response.statusCode} - $responseData");
        throw Exception('Failed to predict sign: ${response.statusCode}');
      }
    } catch (e) {
      print("Network error: $e");
      throw Exception('Error connecting to backend: $e');
    }
  }
}
