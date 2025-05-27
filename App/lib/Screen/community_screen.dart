import 'package:flutter/material.dart';

class CommunityScreen extends StatelessWidget {
  const CommunityScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("쿨쿠리_커뮤니티"),
      ),
      body: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [Text("커뮤니티 탭")],
      ),
    );
  }
}
