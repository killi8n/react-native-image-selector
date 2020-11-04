import * as React from 'react';
import { StyleSheet, Button, SafeAreaView, Image } from 'react-native';
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
          console.log(response);
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
        <Image
          style={{ width: 150, height: 150 }}
          source={{
            uri: response.uri,
          }}
        />
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
});
