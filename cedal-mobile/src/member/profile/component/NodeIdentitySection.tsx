// src/member/profile/components/NodeIdentitySection.tsx
import React from "react";
import { View, Text, StyleSheet, TextInput } from "react-native";

type Props = {
  handle: string;
  nickname: string;
  onChangeNickname: (v: string) => void;
  customLink: string;
  onChangeCustomLink: (v: string) => void;
  age: string;
  onChangeAge: (v: string) => void;
  occupation: string;
  onChangeOccupation: (v: string) => void;
  hobby: string;
  onChangeHobby: (v: string) => void;
  gender: string;
  onChangeGender: (v: string) => void;
  bio: string;
  onChangeBio: (v: string) => void;
};

export function NodeIdentitySection({
  handle,
  nickname,
  onChangeNickname,
  customLink,
  onChangeCustomLink,
  age,
  onChangeAge,
  occupation,
  onChangeOccupation,
  hobby,
  onChangeHobby,
  gender,
  onChangeGender,
  bio,
  onChangeBio,
}: Props) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>Node identity</Text>

      <View style={styles.infoRow}>
        <View style={styles.infoCol}>
          <Text style={styles.infoLabel}>Handle</Text>
          <View style={styles.pillField}>
            <Text style={styles.pillText}>{handle}</Text>
          </View>
        </View>

        <View style={styles.infoCol}>
          <Text style={styles.infoLabel}>Nickname</Text>
          <View style={styles.pillField}>
            <TextInput
              value={nickname}
              onChangeText={onChangeNickname}
              placeholder="Nickname"
              placeholderTextColor="#6b7280"
              style={styles.inputText}
            />
          </View>
        </View>
      </View>

      {/* Custom link */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Custom link</Text>
        <View style={styles.linkField}>
          <TextInput
            value={customLink}
            onChangeText={onChangeCustomLink}
            placeholder={`act://node/${handle}`}
            placeholderTextColor="#6b7280"
            style={[styles.inputText, { flex: 1 }]}
            autoCapitalize="none"
          />
          <Text style={styles.linkHint}>Editable</Text>
        </View>
      </View>

      {/* Age */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Age</Text>
        <View style={styles.pillField}>
          <TextInput
            value={age}
            onChangeText={onChangeAge}
            placeholder="16"
            placeholderTextColor="#6b7280"
            style={styles.inputText}
            keyboardType="number-pad"
          />
        </View>
      </View>

      {/* Occupation */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Occupation</Text>
        <View style={styles.pillField}>
          <TextInput
            value={occupation}
            onChangeText={onChangeOccupation}
            placeholder="Student, dev, etc."
            placeholderTextColor="#6b7280"
            style={styles.inputText}
          />
        </View>
      </View>

      {/* Hobby */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Hobby</Text>
        <View style={styles.pillField}>
          <TextInput
            value={hobby}
            onChangeText={onChangeHobby}
            placeholder="Gaming, coding, streaming…"
            placeholderTextColor="#6b7280"
            style={styles.inputText}
          />
        </View>
      </View>

      {/* Gender */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Gender</Text>
        <View style={styles.pillField}>
          <TextInput
            value={gender}
            onChangeText={onChangeGender}
            placeholder="e.g. Male, Female, Non-binary"
            placeholderTextColor="#6b7280"
            style={styles.inputText}
            autoCapitalize="none"
          />
        </View>
      </View>

      {/* Bio */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Bio</Text>
        <View style={styles.bioBox}>
          <TextInput
            value={bio}
            onChangeText={onChangeBio}
            placeholder="Tell the mesh who you are…"
            placeholderTextColor="#6b7280"
            style={styles.bioInput}
            multiline
            textAlignVertical="top"
          />
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  section: { marginBottom: 16 },
  sectionTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
    marginBottom: 8,
  },
  infoRow: { flexDirection: "row", gap: 8, marginBottom: 8 },
  infoCol: { flex: 1 },
  infoColFull: { marginBottom: 8 },
  infoLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 0.12,
    color: "#9ca3af",
  },
  pillField: {
    marginTop: 3,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  pillText: { fontSize: 12, color: "#e5e7eb" },
  inputText: {
    fontSize: 12,
    color: "#e5e7eb",
    padding: 0,
  },
  linkField: {
    marginTop: 3,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(59,130,246,0.7)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 6,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  linkHint: { fontSize: 11, color: "#9ca3af" },
  bioBox: {
    marginTop: 3,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 7,
  },
  bioInput: {
    fontSize: 12,
    color: "#d1d5db",
    padding: 0,
    minHeight: 60,
  },
});
