import 'package:flutter/material.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("쿨쿠리_홈"),
      ),
      body: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [Text("홈 탭")],
      ),
    );
  }
}
