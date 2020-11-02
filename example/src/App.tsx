import * as React from 'react';
import { StyleSheet, View, Button, Image, Modal, Dimensions, FlatList, SafeAreaView } from 'react-native';
import ImageSelector, { Photo } from 'react-native-image-selector';

export default function App() {
  const [userPhotos, setUserPhotos] = React.useState<Photo[]>([])
  const [modalVisible, setModalVisible] = React.useState<boolean>(false)
  const [isLoading, setIsLoading] = React.useState<boolean>(false)

  React.useEffect(() => {
    if (!modalVisible) {
      setUserPhotos([])
    }
  }, [setUserPhotos, modalVisible])

  const handlePhotos = async () => {
    if (isLoading) {
      return
    }
    setIsLoading(true)
    const photos = await ImageSelector.getPhotos(50)
    setUserPhotos(userPhotos.concat(photos))
    if (!modalVisible) {
      setModalVisible(true)
    }
    setIsLoading(false)
  }

  const handleEndReached = async () => {
    await handlePhotos()
  }

  const handleCloseModal = async () => {
    await ImageSelector.initializePhotos()
    setModalVisible(false)
  }

  return (
    <SafeAreaView style={styles.container}>
      <Button
        title="GET USER PHOTOS"
        onPress={handlePhotos}
      />
      <Modal
        visible={modalVisible}
        presentationStyle="overFullScreen"
        animationType="slide"
      >
        <SafeAreaView style={{ flex: 1 }}>
          <Button title="닫기" onPress={handleCloseModal} />
          <View style={{ flex: 1 }}>
            <FlatList
              style={{
                flex: 1
              }}
              numColumns={3}
              data={userPhotos}
              renderItem={({ item: photo, index }) => {
                return (
                  <Image key={index} style={{ width: Dimensions.get("window").width / 3, height: Dimensions.get("window").height / 6, backgroundColor: "red" }} source={{ uri: photo.uri }} />
                )
              }}
              keyExtractor={(photo, index) => `${index}-${photo.uri}`}
              onEndReached={handleEndReached}
            />
          </View>
        </SafeAreaView>
      </Modal>
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
