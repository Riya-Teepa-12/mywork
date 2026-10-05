module.exports = {
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
  transform: {
    "^.+\\.[jt]sx?$": "babel-jest",
  },
  moduleFileExtensions: ["js", "jsx", "json"],
  testMatch: ["<rootDir>/src/**/__tests__/**/*.(test|spec).[jt]s?(x)"],
  collectCoverageFrom: [
    "src/App.jsx",
    "src/components/**/*.{js,jsx}",
    "src/context/**/*.{js,jsx}",
    "src/lib/**/*.{js,jsx}",
    "!src/components/RichTextEditor.jsx",
  ],
  coveragePathIgnorePatterns: ["/node_modules/", "/dist/"],
  coverageThreshold: {
    global: {
      statements: 80,
      lines: 80,
      functions: 80,
      branches: 70,
    },
  },
};
