import * as React from 'react';
import { StyleSheet, Button, SafeAreaView } from 'react-native';
import ImageSelector from 'react-native-image-selector';

export default function App() {
  const handlePhotos = async () => {
    try {
      console.log('lanchPicker');
      ImageSelector.launchPicker((error, response) => {
        if (error) {
          console.log(error);
          return;
        }
        console.log(response);
      });
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* <Image style={{ width: 50, height: 50 }} source={{ uri: "file:///storage/emulated/0/DCIM/Camera/IMG_20201102_211547.jpg" }} /> */}
      <Button title="GET USER PHOTOS" onPress={handlePhotos} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
