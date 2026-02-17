import 'dart:typed_data';

class AppFile {
  final String name;
  final Uint8List bytes;
  final int size;

  const AppFile({
    required this.name,
    required this.bytes,
    required this.size,
  });

  @override
  String toString() {
    return 'AppFile(name: $name, size: $size bytes)';
  }
}
