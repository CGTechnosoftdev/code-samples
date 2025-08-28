import React, { useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  TouchableOpacity,
  Keyboard,
} from 'react-native';
import { Colors } from '../constants/colors';
import { Strings } from '../constants/strings';
import CustomInput from '../components/CustomInput';
import CustomButton from '../components/CustomButton';
import { AlertHelper } from '../utils/alertHelper';
import Background from '../components/Background';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { OTPScreenSource } from '../constants/navigation';
import { POST } from '../utils/apiHelper';
import { LOGIN_ENDPOINT } from '../constants/api';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Fonts, FontWeights } from '../constants/fonts';

// 📱 Responsive helpers
import { scale, verticalScale, moderateScale } from 'react-native-size-matters';

function LoginScreen({ navigation }: { navigation: any }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{ [key: string]: string }>({});
  const insets = useSafeAreaInsets();

  /** Form validation */
  const validateForm = () => {
    const newErrors: { [key: string]: string } = {};
    if (!email.trim()) {
      newErrors.email = Strings.EMAIL_REQUIRED;
    } else if (!/\S+@\S+\.\S+/.test(email)) {
      newErrors.email = Strings.EMAIL_INVALID;
    }
    if (!password.trim()) {
      newErrors.password = Strings.PASSWORD_REQUIRED;
    } else if (password.length < 6) {
      newErrors.password = Strings.PASSWORD_MIN_LENGTH;
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  /** Login API handler */
  const handleSignIn = async () => {
    Keyboard.dismiss();
    if (!validateForm()) {
      return;
    }
    setLoading(true);
    try {
      const params = {
        username: email.trim(),
        password: password.trim(),
      };

      const response = await POST(LOGIN_ENDPOINT, params);
      console.log('Login response:', response);
      setLoading(false);

      if (response && response.data.status) {
        AlertHelper.showSuccess(response.data.message);
        navigation.navigate('OTP', {
          email,
          password,
          source: OTPScreenSource.LOGIN,
          otp: response.data.otp,
        });
      } else {
        AlertHelper.showError(response.data.message);
      }
    } catch (error: any) {
      console.log('Login error:', error);
      setLoading(false);
      AlertHelper.showError(error.message || Strings.LOGIN_FAILED);
    }
  };

  const handleForgotPassword = () => {
    navigation.navigate('ForgotPassword');
  };

  return (
    <Background>
      <KeyboardAvoidingView
        style={styles.container}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <View style={[styles.mainContainer, { paddingTop: verticalScale(20) }]}>
          <ScrollView
            contentContainerStyle={styles.scrollContent}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}>
            <View style={styles.content}>
              {/* Header */}
              <View style={styles.header}>
                <Text style={styles.HeaderText}>{Strings.LOGIN_WELCOME}</Text>
                <Text style={styles.subtitleText}>{Strings.LOGIN_SUBTITLE}</Text>
              </View>

              {/* Form */}
              <View style={styles.form}>
                <CustomInput
                  label={Strings.EMAIL_LABEL}
                  placeholder={Strings.EMAIL_PLACEHOLDER}
                  value={email}
                  onChangeText={setEmail}
                  keyboardType="email-address"
                  autoCapitalize="none"
                  autoCorrect={false}
                  error={errors.email}
                />

                <CustomInput
                  label={Strings.PASSWORD_LABEL}
                  placeholder={Strings.PASSWORD_PLACEHOLDER}
                  value={password}
                  onChangeText={setPassword}
                  secureTextEntry
                  showEyeIcon
                  error={errors.password}
                />

                <TouchableOpacity
                  style={styles.forgotPasswordButton}
                  onPress={handleForgotPassword}>
                  <Text style={styles.forgotPasswordText}>
                    {Strings.FORGOT_PASSWORD}
                  </Text>
                </TouchableOpacity>
              </View>
            </View>
          </ScrollView>

          {/* Sign In Button */}
          <View
            style={[
              styles.bottomContainer,
              { paddingBottom: verticalScale(24) + insets.bottom },
            ]}>
            <CustomButton
              title={Strings.SIGN_IN}
              onPress={handleSignIn}
              loading={loading}
              style={styles.signInButton}
            />
          </View>
        </View>
      </KeyboardAvoidingView>
    </Background>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  mainContainer: { flex: 1 },
  scrollContent: { flexGrow: 1 },
  content: {
    paddingHorizontal: scale(24),
    paddingVertical: verticalScale(40),
    flex: 1,
  },
  header: {
    alignItems: 'flex-start',
    marginBottom: verticalScale(45),
  },
  HeaderText: {
    fontSize: moderateScale(22),
    color: Colors.TEXT_PRIMARY,
    textAlign: 'center',
    fontFamily: Fonts.BOLD,
    fontWeight: FontWeights.BOLD as '700',
  },
  subtitleText: {
    fontSize: moderateScale(14),
    color: Colors.TEXT_SECONDARY,
    textAlign: 'center',
    fontFamily: Fonts.REGULAR,
    marginTop: verticalScale(4),
  },
  form: { width: '100%' },
  forgotPasswordButton: {
    alignSelf: 'flex-end',
    marginBottom: verticalScale(24),
  },
  forgotPasswordText: {
    color: Colors.BLUE,
    fontSize: moderateScale(13),
    fontWeight: '700',
    fontFamily: Fonts.SEMIBOLD,
  },
  bottomContainer: {
    paddingHorizontal: scale(24),
    paddingTop: verticalScale(20),
  },
  signInButton: { width: '100%' },
});

export default LoginScreen;