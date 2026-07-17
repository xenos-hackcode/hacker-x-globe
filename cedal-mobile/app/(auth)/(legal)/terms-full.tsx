// app/(auth)/(legal)/terms-full.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Dimensions,
} from "react-native";
import { useRouter } from "expo-router";

const { width, height } = Dimensions.get("window");

export default function TermsFullScreen() {
  const router = useRouter();

  return (
    <View style={styles.root}>
      {/* background */}
      <View style={styles.bg} />

      <View style={styles.card}>
        {/* header row with close */}
        <View style={styles.topRow}>
          <TouchableOpacity
            onPress={() => router.back()}
            activeOpacity={0.8}
            style={styles.closeBtn}
          >
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>

          <Text style={styles.headerTitle}>Cedal Legal · Terms & Privacy</Text>
        </View>

        <ScrollView
          style={styles.scroll}
          contentContainerStyle={styles.scrollContent}
          showsVerticalScrollIndicator={true}
        >
          <Text style={styles.mainTitle}>Cedal Terms of Use</Text>
          <Text style={styles.updated}>Last updated: 28 December 2025</Text>
          <Text style={styles.intro}>
            This page contains the full legal text for Cedal access terms and
            privacy. The overview panel on sign-up is only a short summary.
          </Text>

          {/* 1. Introduction */}
          <Text style={styles.sectionTitle}>1. Introduction</Text>
          <Text style={styles.paragraph}>
            These Terms of Use (“Terms”) govern your access to and use of the
            Cedal application, websites, services, software, and content
            (collectively, the “Service”). The Service includes messaging and
            chat features, voice and video communication, live and recorded
            streams, game‑style experiences, social and community tools,
            business and trading tools, and payment and banking‑style
            functionality.
          </Text>
          <Text style={styles.paragraph}>
            By creating an account, accessing, or using any part of the Service,
            you agree to be bound by these Terms. If you do not agree, you must
            not use the Service.
          </Text>

          {/* 2. Changes to these Terms */}
          <Text style={styles.sectionTitle}>2. Changes to these Terms</Text>
          <Text style={styles.paragraph}>
            Cedal may update these Terms from time to time, for example to
            reflect changes to the Service or applicable law. When changes are
            made, Cedal may update the “Last updated” date at the top of this
            page and, where appropriate, provide additional notice in the app.
          </Text>
          <Text style={styles.paragraph}>
            If you continue to use the Service after the updated Terms become
            effective, you will be deemed to have accepted the changes. If you
            do not agree to the updated Terms, you must stop using the Service.
          </Text>

          {/* 3. Eligibility and Accounts */}
          <Text style={styles.sectionTitle}>3. Eligibility and Accounts</Text>
          <Text style={styles.paragraph}>
            You must be legally capable of entering into a binding agreement
            under the laws of your jurisdiction to use the Service. If you use
            the Service on behalf of an organisation, you represent that you are
            authorised to bind that organisation and that it accepts these
            Terms.
          </Text>
          <Text style={styles.paragraph}>
            You are responsible for maintaining the security of your account and
            login credentials. Do not share your password or allow others to
            access your account. You are responsible for all activity that
            occurs under your account, whether or not you authorised it.
          </Text>
          <Text style={styles.paragraph}>
            Cedal may suspend or close accounts that are inactive for an
            extended period, appear compromised, or are used in breach of these
            Terms.
          </Text>

          {/* 4. Acceptable Use */}
          <Text style={styles.sectionTitle}>4. Acceptable Use</Text>
          <Text style={styles.paragraph}>
            You agree to use the Service only for lawful purposes and in a way
            that does not infringe the rights of others or restrict or inhibit
            anyone else’s use of the Service.
          </Text>
          <Text style={styles.paragraph}>You must not, and must not attempt to:</Text>
          <Text style={styles.bullet}>
            • Harass, abuse, threaten, or otherwise harm other users, or
            encourage others to do so.
          </Text>
          <Text style={styles.bullet}>
            • Send spam, scams, or unsolicited commercial messages, or run bots
            or scripts that overload or interfere with the Service.
          </Text>
          <Text style={styles.bullet}>
            • Post or share unlawful, harmful, hateful, infringing, or deceptive
            content, including content that violates intellectual property or
            privacy rights.
          </Text>
          <Text style={styles.bullet}>
            • Reverse engineer, decompile, or attempt to gain unauthorised
            access to any part of the Service or related systems.
          </Text>
          <Text style={styles.bullet}>
            • Use the Service to commit fraud, money laundering, or any other
            illegal activity.
          </Text>

          {/* 5. Virtual Items, Payments, and Trading */}
          <Text style={styles.sectionTitle}>
            5. Virtual Items, Payments, and Trading
          </Text>
          <Text style={styles.paragraph}>
            The Service may allow you to obtain or use virtual items, credits,
            currencies, or other digital assets (“Virtual Items”), through
            purchase, earning, or other mechanisms. Unless stated otherwise,
            Virtual Items have no real‑world monetary value, are not legal
            tender or currency, and cannot be exchanged for cash outside Cedal.
          </Text>
          <Text style={styles.paragraph}>
            Prices, availability, and features of Virtual Items may change at
            any time without notice. Cedal may modify, limit, or remove Virtual
            Items, trading features, or related functionality, including for
            technical, legal, or balancing reasons.
          </Text>
          <Text style={styles.paragraph}>
            Any trading, investment, or marketplace features within the Service
            involve risk. Values can change quickly and you may lose part or all
            of the amount you commit. Cedal does not guarantee execution, price,
            liquidity, or the availability of any particular trade or
            transaction, and may restrict, reverse, or cancel transactions where
            required by law, risk controls, or fraud monitoring.
          </Text>
          <Text style={styles.paragraph}>
            Unless required by law, purchases of Virtual Items or subscriptions
            are generally non‑refundable. Specific refund terms may be provided
            in the app or in your app store’s policies.
          </Text>

          {/* 6. User Content */}
          <Text style={styles.sectionTitle}>6. User Content</Text>
          <Text style={styles.paragraph}>
            The Service may allow you to create, upload, or share content such
            as messages, media, profile information, and other materials (“User
            Content”).
          </Text>
          <Text style={styles.paragraph}>
            You retain ownership of your User Content. However, to operate the
            Service, you grant Cedal a worldwide, non‑exclusive, royalty‑free,
            sublicensable licence to host, store, reproduce, modify, adapt,
            publish, display, and distribute your User Content solely for the
            purpose of operating, improving, and promoting the Service.
          </Text>
          <Text style={styles.paragraph}>
            You are responsible for your User Content and for ensuring it
            complies with these Terms and applicable law. Cedal may remove or
            restrict access to any User Content that appears to violate these
            Terms or pose risk to users or the Service.
          </Text>

          {/* 7. Intellectual Property */}
          <Text style={styles.sectionTitle}>7. Intellectual Property</Text>
          <Text style={styles.paragraph}>
            The Service, including all software, design, text, graphics, logos,
            and other content, is owned by Cedal or its licensors and is
            protected by intellectual property laws. Except as allowed by these
            Terms, you must not copy, modify, distribute, sell, or lease any
            part of the Service.
          </Text>
          <Text style={styles.paragraph}>
            You may not use Cedal’s name, logo, or branding without prior
            written permission, except as allowed by applicable law.
          </Text>

          {/* 8. Privacy and Data */}
          <Text style={styles.sectionTitle}>8. Privacy and Data</Text>
          <Text style={styles.paragraph}>
            Cedal collects and uses personal data as described in the Cedal
            Privacy Policy, which forms part of these Terms. The Privacy Policy
            explains what data is collected, how it is used, and your choices
            and rights.
          </Text>
          <Text style={styles.paragraph}>
            Certain features may request permissions such as camera,
            microphone, contacts, or approximate location. You may choose to
            deny these permissions, but some features may not function without
            them.
          </Text>

          {/* 9. Third‑Party Services */}
          <Text style={styles.sectionTitle}>9. Third‑Party Services</Text>
          <Text style={styles.paragraph}>
            The Service may integrate with or link to third‑party services,
            content, or sites. Cedal does not control and is not responsible for
            third‑party services. Your use of them may be subject to separate
            terms and policies.
          </Text>

          {/* 10. “AS IS” and “AS AVAILABLE” */}
          <Text style={styles.sectionTitle}>10. “AS IS” and “AS AVAILABLE”</Text>
          <Text style={styles.paragraph}>
            To the maximum extent permitted by law, the Service is provided “as
            is” and “as available”, without any warranties of any kind, whether
            express or implied, including implied warranties of merchantability,
            fitness for a particular purpose, and non‑infringement.
          </Text>
          <Text style={styles.paragraph}>
            Cedal does not guarantee that the Service will be uninterrupted,
            secure, error‑free, or free of harmful components, or that any
            defects will be corrected.
          </Text>

          {/* 11. Limitation of Liability */}
          <Text style={styles.sectionTitle}>11. Limitation of Liability</Text>
          <Text style={styles.paragraph}>
            To the maximum extent permitted by law, Cedal and its affiliates
            will not be liable for any indirect, incidental, special,
            consequential, or punitive damages, or for any loss of profits or
            revenues, whether incurred directly or indirectly, or any loss of
            data, use, goodwill, or other intangible losses, resulting from your
            use of or inability to use the Service.
          </Text>
          <Text style={styles.paragraph}>
            To the extent Cedal is found liable, its total liability for all
            claims relating to the Service will be limited to the greater of
            (a) the amount you paid Cedal for the Service in the 3 months before
            the event giving rise to the claim, or (b) a reasonable fixed amount
            stated in your final policy.
          </Text>

          {/* 12. Suspension and Termination */}
          <Text style={styles.sectionTitle}>12. Suspension and Termination</Text>
          <Text style={styles.paragraph}>
            Cedal may suspend or terminate your access to the Service, or remove
            any User Content, at any time where reasonably necessary, including
            for violation of these Terms, suspected fraud or abuse, legal or
            safety reasons, or technical risk to the Service.
          </Text>
          <Text style={styles.paragraph}>
            You may stop using the Service and close your account at any time.
            Some obligations (for example, payment obligations, licences
            granted, and limitation of liability) will continue after
            termination.
          </Text>

          {/* 13. Governing Law and Disputes */}
          <Text style={styles.sectionTitle}>
            13. Governing Law and Disputes
          </Text>
          <Text style={styles.paragraph}>
            These Terms and any disputes arising out of or relating to them or
            the Service are governed by the laws you specify in your final
            policy, without regard to its conflict‑of‑law rules.
          </Text>
          <Text style={styles.paragraph}>
            You agree that any claims or disputes will be resolved in the
            courts or dispute resolution forum you specify, except where
            applicable law provides a different mandatory forum.
          </Text>

          {/* 14–20 etc. You can keep adding your remaining sections here as you had them. */}
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    alignItems: "center",
    justifyContent: "center",
  },
  bg: {
    position: "absolute",
    width,
    height,
    backgroundColor: "#000",
  },
  card: {
    width: Math.min(width * 0.96, 820),
    height: Math.min(height * 0.96, height - 32),
    backgroundColor: "rgba(15,23,42,0.96)",
    borderRadius: 24,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.4)",
    padding: 18,
  },
  topRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 10,
  },
  closeBtn: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "#1e3a8a",
  },
  closeText: {
    fontSize: 11,
    color: "#e5e7eb",
  },
  headerTitle: {
    fontSize: 11,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 2,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 24,
  },
  mainTitle: {
    fontSize: 24,
    fontWeight: "700",
    color: "#e5e7eb",
    marginBottom: 4,
  },
  updated: {
    fontSize: 13,
    color: "#9ca3af",
    marginBottom: 8,
  },
  intro: {
    fontSize: 13,
    color: "#9ca3af",
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: "#e5e7eb",
    marginTop: 22,
  },
  paragraph: {
    fontSize: 14,
    color: "#e5e7eb",
    marginTop: 8,
    lineHeight: 20,
  },
  bullet: {
    fontSize: 14,
    color: "#e5e7eb",
    marginTop: 6,
    lineHeight: 20,
    paddingLeft: 8,
  },
});
