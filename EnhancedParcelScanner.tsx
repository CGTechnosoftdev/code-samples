import React, { useEffect, useState, useRef, useCallback } from 'react';
import { 
  View, 
  Text, 
  StyleSheet, 
  Alert, 
  Image, 
  TouchableOpacity,
  ActivityIndicator,
  Modal
} from 'react-native';
import { Camera, useCameraDevices } from 'react-native-vision-camera';
import { CameraHighlights, useBarcodeScanner } from '@mgcrea/vision-camera-barcode-scanner';
import { Worklets } from 'react-native-worklets-core';
import { launchCamera, ImagePickerResponse, MediaType } from 'react-native-image-picker';
import OCRService from '../utils/ocrService';
import { Colors } from '../constants/colors';
import { Strings } from '../constants/strings';
import { requestCameraPermission } from '../utils/permissions';

//  size-matters
import { scale, verticalScale, moderateScale } from 'react-native-size-matters';

interface EnhancedParcelScannerProps {
  onScanned?: (value: string) => void;
  showScannedValue?: boolean;
  isScanning?: boolean;
  scanLabel?: string;
}

type ScanMode = 'barcode' | 'ocr';

const EnhancedParcelScanner: React.FC<EnhancedParcelScannerProps> = ({ 
  onScanned, 
  showScannedValue = true, 
  isScanning = true, 
  scanLabel 
}) => {
  const cameraRef = useRef<Camera>(null);
  const [hasPermission, setHasPermission] = useState(false);
  const [scannedValue, setScannedValue] = useState<string | null>(null);
  const [scanMode, setScanMode] = useState<ScanMode>('barcode');
  const [isProcessingOCR, setIsProcessingOCR] = useState(false);

  const devices = useCameraDevices();
  const device = devices.find((d) => d.position === 'back');

  // 📷 Request permission on mount
  useEffect(() => {
    (async () => {
      const granted = await requestCameraPermission();
      setHasPermission(granted);
    })();
  }, []);

  /** Barcode scanning callback */
  const onCaptured = (codes: any[]) => {
    const value = codes[0]?.value;
    if (value) {
      setScannedValue(value);
      if (onScanned) onScanned(value);
      setTimeout(() => setScannedValue(null), 1000);
    }
  };
  const onCapturedJS = Worklets.createRunOnJS(onCaptured);

  /** Barcode scanner setup */
  const { props: cameraProps, highlights } = useBarcodeScanner({
    fps: 5,
    barcodeTypes: ['qr', 'ean-13', 'code-128', 'pdf-417'],
    onBarcodeScanned: codes => {
      'worklet';
      onCapturedJS(codes);
    },
  });

  /** OCR image capture */
  const handleCameraCapture = useCallback(async () => {
    try {
      const result: ImagePickerResponse = await launchCamera({
        mediaType: 'photo' as MediaType,
        quality: 0.8,
        includeBase64: true,
        saveToPhotos: false,
      });
      if (result.assets?.[0]?.base64) {
        await processOCRImage(result.assets[0].base64);
      }
    } catch (error) {
      Alert.alert('Error', Strings.OCR_ERROR);
    }
  }, []);

  /** Process OCR */
  const processOCRImage = async (base64Image: string) => {
    if (!base64Image) return;
    setIsProcessingOCR(true);

    try {
      const ocrResults = await OCRService.extractTextFromImage(base64Image);
      const parsedResult = OCRService.parseIDFromText(ocrResults);
      
      if (parsedResult?.isValid) {
        setScannedValue(parsedResult.id);
        if (onScanned) onScanned(parsedResult.id);
        setTimeout(() => setScannedValue(null), 1000);
      } else {
        Alert.alert(Strings.CAMERA_PERMISSION_TITLE, Strings.OCR_NO_VALID_ID);
      }
    } catch {
      Alert.alert('OCR Error', Strings.OCR_ERROR);
    } finally {
      setIsProcessingOCR(false);
    }
  };

  const toggleScanMode = () => {
    setScanMode(prev => (prev === 'barcode' ? 'ocr' : 'barcode'));
  };

  const getScanLabel = () => {
    if (scanLabel === Strings.SCAN_SUCCESS) return scanLabel;
    return scanMode === 'barcode'
      ? scanLabel || Strings.BARCODE_LABEL
      : scanLabel || Strings.OCR_LABEL;
  };

  if (!device || !hasPermission) {
    return (
      <View style={styles.centered}>
        <Text>{Strings.LOADING_CAMERA}</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* Mode Toggle */}
      <TouchableOpacity 
        style={styles.modeToggleButton}
        onPress={toggleScanMode}
      >
        <Text style={styles.modeToggleText}>
          {scanMode === 'barcode' ? '📱 Barcode' : '📷 OCR'}
        </Text>
        <Text style={styles.modeToggleSubtext}>
          Tap to switch to {scanMode === 'barcode' ? 'OCR' : 'Barcode'}
        </Text>
      </TouchableOpacity>

      {/* Barcode Mode */}
      {scanMode === 'barcode' && (
        <View style={styles.barcodeModeContainer}>
          <Camera
            ref={cameraRef}
            style={styles.scanArea}
            device={device}
            isActive={isScanning}
            {...cameraProps}
          />
          <CameraHighlights highlights={highlights} color={Colors.SUCCESS} />
        </View>
      )}

      {/* OCR Mode */}
      {scanMode === 'ocr' && (
        <TouchableOpacity 
          style={styles.ocrModeContainer}
          onPress={handleCameraCapture}
        >
          <Image 
            source={require('../assets/images/cameraPlaceholder.png')} 
            style={styles.ocrIcon}
            resizeMode="contain"
          />
        </TouchableOpacity>
      )}

      {/* Scan Label */}
      <View style={styles.scanLabelContainer}>
        <Text 
          style={[
            styles.scanLabelText, 
            { color: scanMode === 'barcode' ? Colors.WHITE : Colors.BLACK }
          ]}
        >
          {getScanLabel()}
        </Text>
      </View>

      {/* OCR Loader */}
      <Modal visible={isProcessingOCR} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <ActivityIndicator size="large" color={Colors.THEME} />
            <Text style={styles.modalText}>{Strings.OCR_PROCESSING}</Text>
            <Text style={styles.modalSubtext}>{Strings.OCR_SUBTEXT}</Text>
          </View>
        </View>
      </Modal>
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1 },
  centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },

  modeToggleButton: {
    position: 'absolute',
    top: verticalScale(20),
    right: scale(16),
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    paddingHorizontal: scale(14),
    paddingVertical: verticalScale(6),
    borderRadius: moderateScale(20),
    zIndex: 20,
    alignItems: 'center',
  },
  modeToggleText: {
    color: Colors.WHITE,
    fontSize: moderateScale(16),
    fontWeight: 'bold',
  },
  modeToggleSubtext: {
    color: Colors.WHITE,
    fontSize: moderateScale(10),
    opacity: 0.8,
    marginTop: verticalScale(2),
  },

  scanArea: {
    borderRadius: moderateScale(16),
    overflow: 'hidden',
    width: scale(320),
    height: verticalScale(400),
    backgroundColor: Colors.BLACK,
    alignSelf: 'center',
    marginBottom: verticalScale(20),
  },
  barcodeModeContainer: {
    width: scale(320),
    height: verticalScale(400),
    alignSelf: 'center',
  },
  ocrModeContainer: {
    width: scale(320),
    height: verticalScale(400),
    backgroundColor: Colors.LIGHT_GRAY,
    borderRadius: moderateScale(16),
    alignSelf: 'center',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: verticalScale(60),
  },
  ocrIcon: {
    width: scale(60),
    height: scale(60),
    opacity: 0.6,
  },
  scanLabelContainer: {
    position: 'absolute',
    bottom: verticalScale(30),
    left: 0,
    right: 0,
    alignItems: 'center',
  },
  scanLabelText: {
    fontSize: moderateScale(16),
    textAlign: 'center',
  },

  modalOverlay: {
    flex: 1,
    backgroundColor: Colors.BACKDROP,
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: Colors.WHITE,
    padding: scale(20),
    borderRadius: moderateScale(16),
    alignItems: 'center',
    minWidth: scale(220),
  },
  modalText: {
    fontSize: moderateScale(16),
    fontWeight: '600',
    color: Colors.BLACK,
    marginTop: verticalScale(12),
    marginBottom: verticalScale(6),
  },
  modalSubtext: {
    fontSize: moderateScale(12),
    color: '#666',
    textAlign: 'center',
  },
});

export default EnhancedParcelScanner;