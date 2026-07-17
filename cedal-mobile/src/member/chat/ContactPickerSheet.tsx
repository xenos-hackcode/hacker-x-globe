// src/member/chat/ContactPickerSheet.tsx
import React, { useMemo, useState } from "react";
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  ScrollView,
} from "react-native";

type ContactUser = {
  id: string;
  name: string;
  handle: string;
};

type Props = {
  visible: boolean;
  meId: string;
  currentPeerId: string | null;
  contacts: ContactUser[]; // all DM contacts
  onClose: () => void;
  onPickContact: (user: ContactUser) => void;
};

export function ContactPickerSheet({
  visible,
  meId,
  currentPeerId,
  contacts,
  onClose,
  onPickContact,
}: Props) {
  const [search, setSearch] = useState("");

  if (!visible) return null;

  const filtered = useMemo(() => {
  const q = search.trim().toLowerCase();
  return contacts.filter((u) => {
    if (!u.id || u.id === meId) return false;
    if (currentPeerId && u.id === currentPeerId) return false;
    if (!q) return true;
    return (
      u.name.toLowerCase().includes(q) ||
      u.handle.toLowerCase().includes(q)
    );
  });
}, [contacts, search, meId, currentPeerId]);

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.headerRow}>
            <Text style={styles.title}>Contacts</Text>
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={onClose}
              style={styles.closeBtn}
            >
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.searchBox}>
            <TextInput
              value={search}
              onChangeText={setSearch}
              placeholder="Search contacts…"
              placeholderTextColor="#6b7280"
              style={styles.searchInput}
            />
          </View>

          <View style={{ height: 320 }}>
            <ScrollView
              showsVerticalScrollIndicator={false}
              contentContainerStyle={styles.listContent}
            >
              {filtered.map((u) => (
  <TouchableOpacity
    key={u.id}
    style={styles.item}
    activeOpacity={0.8}
    onPress={() => onPickContact(u)}
  >
    <Text style={styles.name}>{u.name}</Text>
    <Text style={styles.handleText}>{u.handle}</Text>
  </TouchableOpacity>
))}

              {filtered.length === 0 && (
                <View style={styles.emptyState}>
                  <Text style={styles.emptyText}>
                    No contacts available to share.
                  </Text>
                </View>
              )}
            </ScrollView>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(15,23,42,0.75)",
  },
  sheet: {
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 24,
    flexShrink: 0,
    maxHeight: 700,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "700",
  },
  closeBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  closeText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  searchBox: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 12,
    paddingVertical: 6,
    marginBottom: 8,
  },
  searchInput: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  listContent: {
    paddingBottom: 8,
  },
  item: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.8)",
  },
  name: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  emptyState: {
    width: "100%",
    paddingVertical: 20,
    alignItems: "center",
  },
  emptyText: {
    color: "#6b7280",
    fontSize: 12,
  },
  handleText: {
  fontSize: 11,
  color: "#9ca3af",
},
});
