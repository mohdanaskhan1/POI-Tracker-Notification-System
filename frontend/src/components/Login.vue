<template>
  <div class="login-wrapper">
    <header class="app-header">
      <h1>Location-Based POI Detection</h1>
    </header>
    <div class="login-container">
      <div class="tab-bar">
        <button type="button" :class="['tab', !isRegistering ? 'active' : '']" @click="isRegistering = false">Login</button>
        <button type="button" :class="['tab', isRegistering ? 'active' : '']" @click="isRegistering = true">Register</button>
      </div>
      <div class="hero">
        <h2>{{ isRegistering ? 'Create your account' : 'Welcome back' }}</h2>
        <p>{{ isRegistering ? 'Sign up to get smart POI alerts near you.' : 'Log in to continue exploring nearby POIs.' }}</p>
      </div>
      <form @submit.prevent="isRegistering ? handleRegister() : handleLogin()">
        <input v-if="isRegistering" v-model="name" type="text" placeholder="Full Name" required />
        <span v-if="errors.name" class="error">{{ errors.name }}</span>
        <input v-model="username" type="text" placeholder="Username" required />
        <span v-if="errors.username" class="error">{{ errors.username }}</span>
        <input v-model="password" type="password" placeholder="Password" required />
        <span v-if="errors.password" class="error">{{ errors.password }}</span>
        <button type="submit">{{ isRegistering ? 'Register' : 'Login' }}</button>
        <p v-if="error" class="error">{{ error }}</p>
        <p v-if="successMessage" class="success">{{ successMessage }}</p>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import apiClient, { getApiBaseUrl } from '../utils/api';

const name = ref('');
const username = ref('');
const password = ref('');
const error = ref(null);
const errors = ref({});
const successMessage = ref(null);
const isRegistering = ref(false);
const emit = defineEmits(['loggedIn']);

async function handleLogin() {
  error.value = null;
  errors.value = {};
  successMessage.value = null;
  try {
    const response = await apiClient.post(`${getApiBaseUrl()}/api/auth/login`, {
      username: username.value,
      password: password.value,
    });
    const { userId } = response.data;
    if (userId) {
      localStorage.setItem('userId', userId);
      emit('loggedIn');
    } else {
      error.value = 'Login failed: No user ID returned.';
    }
  } catch (err) {
    error.value = 'Invalid credentials. Please try again.';
    console.error('Login error:', err);
  }
}

async function handleRegister() {
  error.value = null;
  errors.value = {};
  successMessage.value = null;
  try {
    await apiClient.post(`${getApiBaseUrl()}/api/auth/register`, {
      name: name.value,
      username: username.value,
      password: password.value,
    });
    successMessage.value = 'Registration successful! Please log in.';
    isRegistering.value = false; // Switch back to login form
  } catch (err) {
    if (err.response && err.response.status === 400) {
      errors.value = err.response.data;
    } else {
      error.value = err.response?.data || 'Registration failed. Please try again.';
    }
    console.error('Registration error:', err);
  }
}
</script>

<style scoped>
.login-wrapper { min-height: 100vh; display: flex; flex-direction: column; background: linear-gradient(135deg, #f0fdf4 0%, #edf2f7 100%); padding-inline: 12px; box-sizing: border-box; }
.app-header { padding: 16px; background: #2f855a; color: white; }
.app-header h1 { margin: 0; font-size: 18px; }
.login-container {
  width: min(100%, 440px);
  margin: clamp(16px, 6vh, 40px) auto;
  padding: clamp(16px, 4vw, 24px);
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 10px 25px rgba(0,0,0,0.08);
  box-sizing: border-box;
}
.tab-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 8px;
}
.tab {
  flex: 1;
  padding: 10px 12px;
  background: #f7fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  color: #2d3748;
}
.tab.active {
  background: #2b6cb0;
  color: #fff;
  border-color: #2b6cb0;
}
.hero { text-align: center; margin: 8px 0 16px; }
.hero h2 { margin: 0; font-size: 26px; font-weight: 800; background: linear-gradient(90deg, #2b6cb0, #68d391); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.hero p { margin: 6px 0 0; font-size: 14px; color: #4a5568; }
form { display: grid; grid-template-columns: 1fr; gap: 12px; }
input {
  width: 100%;
  padding: 12px;
  border: 1px solid #cbd5e0;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}
input:focus { outline: none; border-color: #2b6cb0; box-shadow: 0 0 0 3px rgba(43,108,176,0.15); }
button[type="submit"] {
  width: 100%;
  padding: 12px;
  background: linear-gradient(90deg, #2b6cb0, #2f855a);
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(47,133,90,0.25);
  transition: transform 0.08s ease-in-out, filter 0.15s ease;
}
.login-container button[type="submit"]:hover { filter: brightness(1.05); }
.login-container button[type="submit"]:active { transform: translateY(1px); }
.error { color: #b83232; margin-top: 6px; font-size: 13px; text-align: left; }
.success { color: #2f855a; margin-top: 6px; font-size: 13px; text-align: left; }
.toggle-button {
  background: none;
  border: none;
  color: #2b6cb0;
  cursor: pointer;
  margin-top: 12px;
  padding: 0;
  font-weight: 600;
}
@media (max-width: 480px) {
  .login-container { margin: 24px auto; padding: 16px; width: 100%; }
}
</style>
