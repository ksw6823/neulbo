import 'package:flutter/material.dart';

class MyProfileScreen extends StatelessWidget {
  const MyProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("쿨쿠리_내 정보"),
      ),
      body: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [Text("내 정보 탭")],
      ),
    );
  }
}
