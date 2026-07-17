// app/(auth)/(member)/index.tsx
import { Redirect } from "expo-router";

export default function MemberIndex() {
  return <Redirect href="/(auth)/(member)/home" />;
}
