import * as React from 'react';
import { StyleSheet, Button, SafeAreaView, Image, Text } from 'react-native';
import ImageSelector, {
  ImageSelectorCallbackResponse,
} from 'react-native-image-selector';

export default function App() {
  const [
    response,
    setResponse,
  ] = React.useState<ImageSelectorCallbackResponse | null>(null);
  const handlePhotos = async () => {
    try {
      ImageSelector.launchPicker(
        (error, response: ImageSelectorCallbackResponse) => {
          if (error) {
            console.log(error);
            return;
          }
          setResponse(response);
        }
      );
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <Button title="GET USER PHOTOS" onPress={handlePhotos} />
      {response !== null && (
        <>
          <Text style={styles.textStyle}>Base64 Image</Text>
          <Image
            style={styles.imageStyle}
            source={{
              uri: `data:image/png;base64,${response.data}`,
            }}
          />
          <Text style={styles.textStyle}>URI Image</Text>
          <Image
            style={styles.imageStyle}
            source={{
              uri: response.uri,
            }}
          />
          <Text style={styles.textStyle}>path: {response.path}</Text>
          <Text style={styles.textStyle}>fileName: {response.fileName}</Text>
          <Text style={styles.textStyle}>fileSize: {response.fileSize}</Text>
          <Text style={styles.textStyle}>fileType: {response.type}</Text>
        </>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  textStyle: {
    marginTop: 15,
  },
  imageStyle: {
    width: 150,
    height: 150,
    borderWidth: 1,
    borderColor: '#ababab',
  },
});
