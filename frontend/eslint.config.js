import eslint from '@eslint/js'
import vue from 'eslint-plugin-vue'
import globals from 'globals'

export default [
  {
    ignores: [
      'dist/',
      'node_modules/',
    ],
  },

  eslint.configs.recommended,

  ...vue.configs['flat/essential'],

  {
    files: ['**/*.{js,vue}'],

    languageOptions: {
      globals: {
        ...globals.browser,
      },
    },

    rules: {
      // Code-quality rules
      'no-unused-vars': 'warn',

      // Vue naming
      'vue/multi-word-component-names': 'off',

      // Formatting rules
      'vue/html-indent': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/html-self-closing': 'off',
    },
  },

  {
    files: ['vite.config.js'],
    languageOptions: {
      globals: {
        ...globals.node,
      },
    },
  },
]