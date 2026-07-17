import React from "react";
import { View, Text, StyleSheet, TextInput, TouchableOpacity } from "react-native";

type Props = {
  handle: string;
  nickname: string;
  onChangeNickname: (value: string) => void;
  customLink: string;
  age: string;
  occupation: string;
  hobby: string;
  gender: string;
  bio: string;
  onCopy: (text: string) => void;
};

export function NodeIdentitySection({
  handle,
  nickname,
  onChangeNickname,
  customLink,
  age,
  occupation,
  hobby,
  gender,
  bio,
  onCopy,
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

      {/* custom link + copy */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Custom link</Text>
        <View style={styles.pillField}>
          <Text style={styles.pillText}>{customLink}</Text>
        </View>

        <TouchableOpacity
          style={styles.copyBtn}
          activeOpacity={0.8}
          onPress={() => onCopy(customLink)}
        >
          <Text style={styles.copyText}>Copy custom link</Text>
        </TouchableOpacity>
      </View>

      {/* Age */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Age</Text>
        <View style={styles.pillField}>
          <Text style={styles.pillText}>{age}</Text>
        </View>
      </View>

      {/* Occupation */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Occupation</Text>
        <View style={styles.pillField}>
          <Text style={styles.pillText}>{occupation}</Text>
        </View>
      </View>

      {/* Hobby */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Hobby</Text>
        <View style={styles.pillField}>
          <Text style={styles.pillText}>{hobby}</Text>
        </View>
      </View>

      {/* Gender */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Gender</Text>
        <View style={styles.pillField}>
          <Text style={styles.pillText}>{gender}</Text>
        </View>
      </View>

      {/* Bio */}
      <View style={styles.infoColFull}>
        <Text style={styles.infoLabel}>Bio</Text>
        <View style={styles.bioBox}>
          <Text style={styles.bioText}>{bio}</Text>
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
  inputText: { fontSize: 12, color: "#e5e7eb", padding: 0 },
  bioBox: {
    marginTop: 3,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 7,
  },
  bioText: { fontSize: 12, color: "#d1d5db" },
  copyBtn: {
    marginTop: 6,
    alignSelf: "flex-start",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(59,130,246,0.9)",
    backgroundColor: "#0b1120",
  },
  copyText: {
    fontSize: 11,
    color: "#bfdbfe",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
});
