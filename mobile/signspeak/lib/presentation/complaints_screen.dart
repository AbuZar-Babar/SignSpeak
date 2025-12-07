import 'package:flutter/material.dart';

class ComplaintsScreen extends StatelessWidget {
  const ComplaintsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Row(
          children: [
            Hero(tag: 'Complaints', child: Icon(Icons.report_problem)),
            SizedBox(width: 10),
            Text('Complaints'),
          ],
        ),
      ),
      body: const Center(child: Text('No complaints yet')),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // Show add complaint dialog
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
