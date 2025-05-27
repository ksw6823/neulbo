import 'package:flutter/material.dart';

class SleepAIScreen extends StatelessWidget {
  const SleepAIScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("쿨쿠리_수면 비서"),
      ),
      body: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [Text("수면 비서 탭")],
      ),
    );
  }
}
