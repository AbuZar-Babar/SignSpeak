// import 'package:firebase_auth/firebase_auth.dart';

class AuthService {
  // final FirebaseAuth _auth = FirebaseAuth.instance;

  // Stream<User?> get authStateChanges => _auth.authStateChanges();

  Future<bool> signIn(String email, String password) async {
    await Future.delayed(const Duration(seconds: 1)); // Simulate network delay
    return true;
  }

  Future<bool> register(String email, String password) async {
    await Future.delayed(const Duration(seconds: 1)); // Simulate network delay
    return true;
  }

  Future<void> signOut() async {
    // await _auth.signOut();
  }

  // User? get currentUser => _auth.currentUser;
}
