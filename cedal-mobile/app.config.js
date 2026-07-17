// app.config.js
import appJson from "./app.json";

export default {
  ...appJson,
  expo: {
    ...appJson.expo,
    plugins: [
      ...(appJson.expo?.plugins ?? []),
      "expo-secure-store",
    ],
    extra: {
      ...appJson.expo.extra,
      OPENAI_API_KEY: process.env.OPENAI_API_KEY,
    },
  },
};
