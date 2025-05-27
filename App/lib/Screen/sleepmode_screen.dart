import 'package:flutter/material.dart';

class SleepModeScreen extends StatelessWidget {
  const SleepModeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("쿨쿠리_수면모드"),
      ),
      body: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [Text("수면모드 탭")],
      ),
    );
  }
}
