// src/member/chat/partials/ChatSheets.tsx
import React from "react";
import { MediaPreviewSheet } from "@/src/member/utils/MediaPreviewSheet";
import { ViewOnceConfigSheet } from "@/src/member/utils/ViewOnceConfigSheet";
import { ViewOnceOverlay } from "../ViewOnceOverlay";
import { VoiceRecorderSheet } from "../VoiceRecorderSheet";
import { VoicePreviewSheet } from "../VoicePreviewSheet";
import { VoiceEditSheet } from "../VoiceEditSheet";
import { StickerPickerSheet } from "../StickerPickerSheet";
import { StickerPreviewSheet } from "../StickerPreviewSheet";
import { ChatMediaGallery } from "../ChatMediaGallery";
import { ChatAudioGallery } from "../ChatAudioGallery";
import { LinkPickerSheet } from "../LinkPickerSheet";
import { ContactPickerSheet } from "../ContactPickerSheet";
import { PollComposerSheet } from "../PollComposerSheet";
import { EventComposerSheet } from "../EventComposerSheet";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";

type Props = {
  state: ReturnType<typeof useChatScreenState>;
};

export function ChatSheets({ state }: Props) {
  const {
    chatId,
    otherUserId,
    currentUserId,

    // view-once preview
    previewUri,
    previewViewOnce,
    setPreviewViewOnce,
    setPreviewUri,
    viewOnceConfig,
    setViewOnceConfig,
    viewOnceConfigOpen,
    setViewOnceConfigOpen,

    // open view-once msg
    openViewOnceMsg,

    // voice
    voiceSheetOpen,
    setVoiceSheetOpen,
    audioPreviewOpen,
    setAudioPreviewOpen,
    pendingAudioUri,
    editConfig,
    editSheetOpen,
    setEditSheetOpen,
    editDurationMs,

    // stickers
    stickerSheetOpen,
    setStickerSheetOpen,
    stickerPreviewUrl,
    setStickerPreviewUrl,
    stickerPreviewViewOnce,
    setStickerPreviewViewOnce,

    // galleries
    galleryOpen,
    setGalleryOpen,
    galleryItems,
    audioPanelOpen,
    setAudioPanelOpen,
    audioItems,

    // link / contacts / poll / event
    linkPickerOpen,
    setLinkPickerOpen,
    groups,
    setInput,
    contactPickerOpen,
    setContactPickerOpen,
    dmContacts,
    pollSheetOpen,
    setPollSheetOpen,
    eventSheetOpen,
    setEventSheetOpen,
  } = state as any;

  const {
    handleViewOnceClose,
    handleAudioRecorded,
    handleAudioSend,
    handleAudioDiscard,
    handleAudioEdit,
    handleApplyEdit,
    handlePreviewSend,
    handlePreviewCancel,
    sendStickerFromPreview,
    sendContact,
    sendPoll,
    sendEvent,
  } = state as any;

  return (
    <>
      {/* MEDIA PREVIEW */}
      {previewUri && (
        <MediaPreviewSheet
          uri={previewUri}
          viewOnce={previewViewOnce}
          onToggleViewOnce={() => {
            setPreviewViewOnce(true);
            setViewOnceConfigOpen(true);
          }}
          onSend={handlePreviewSend}
          onCancel={handlePreviewCancel}
        />
      )}

      {/* VIEW-ONCE CONFIG */}
      {viewOnceConfigOpen && (
        <ViewOnceConfigSheet
          visible={viewOnceConfigOpen}
          value={viewOnceConfig}
          onChange={setViewOnceConfig}
          onClose={() => setViewOnceConfigOpen(false)}
          onCancelViewOnce={() => {
            setPreviewViewOnce(false);
            setStickerPreviewViewOnce(false);
            setViewOnceConfig({
              mode: "views",
              maxViews: 1,
              timeLimitSeconds: null,
            });
          }}
        />
      )}

      {/* OPEN VIEW-ONCE OVERLAY */}
      {openViewOnceMsg &&
        (openViewOnceMsg.imageUri ||
          openViewOnceMsg.videoUri ||
          openViewOnceMsg.audioUri) && (
          <ViewOnceOverlay
            message={openViewOnceMsg}
            onClose={() => handleViewOnceClose(openViewOnceMsg)}
          />
        )}

      {/* VOICE SHEETS */}
      <VoiceRecorderSheet
        visible={voiceSheetOpen}
        onClose={() => setVoiceSheetOpen(false)}
        onRecorded={handleAudioRecorded}
      />

      <VoicePreviewSheet
        visible={audioPreviewOpen}
        uri={pendingAudioUri}
        editConfig={editConfig}
        viewOnce={previewViewOnce}
        onToggleViewOnce={() => {
          setPreviewViewOnce(true);
          setViewOnceConfigOpen(true);
        }}
        onClose={() => setAudioPreviewOpen(false)}
        onSend={handleAudioSend}
        onDiscard={handleAudioDiscard}
        onEdit={handleAudioEdit}
      />

      <VoiceEditSheet
        visible={editSheetOpen}
        durationMs={editDurationMs}
        onClose={() => setEditSheetOpen(false)}
        onApply={handleApplyEdit}
      />

      {/* STICKERS */}
      <StickerPickerSheet
        visible={stickerSheetOpen}
        onClose={() => setStickerSheetOpen(false)}
        onSelectSticker={(url) => {
          setStickerSheetOpen(false);
          setStickerPreviewUrl(url);
          setStickerPreviewViewOnce(false);
        }}
        onOpenChatMedia={() => {}}
      />

      {stickerPreviewUrl && (
        <StickerPreviewSheet
          url={stickerPreviewUrl}
          viewOnce={stickerPreviewViewOnce}
          onToggleViewOnce={() => {
            setStickerPreviewViewOnce(true);
            setViewOnceConfigOpen(true);
          }}
          onCancel={() => {
            setStickerPreviewUrl(null);
            setStickerPreviewViewOnce(false);
          }}
          onSend={sendStickerFromPreview}
        />
      )}

      {/* MEDIA / AUDIO GALLERIES */}
      <ChatMediaGallery
        visible={galleryOpen}
        items={galleryItems}
        onClose={() => setGalleryOpen(false)}
      />

      <ChatAudioGallery
        visible={audioPanelOpen}
        items={audioItems}
        onClose={() => setAudioPanelOpen(false)}
      />

      {/* LINK PICKER */}
     <LinkPickerSheet
  visible={linkPickerOpen}
  groups={groups}
  onClose={() => setLinkPickerOpen(false)}
  onPickLink={(link) => {
    setInput((prev: string) => (prev ? `${prev} ${link}` : link));
  }}
/>

<ContactPickerSheet
  visible={contactPickerOpen}
  meId={currentUserId}
  currentPeerId={otherUserId ?? null}
  contacts={dmContacts}
  onClose={() => setContactPickerOpen(false)}
  onPickContact={(user) => {
    if (!user.handle) return;
    setInput((prev: string) => {
      const base = prev && !prev.endsWith(" ") ? prev + " " : prev;
      return (base || "") + user.handle;
    });
    setContactPickerOpen(false);
  }}
/>

      {/* POLL */}
      <PollComposerSheet
        visible={pollSheetOpen}
        onClose={() => setPollSheetOpen(false)}
        onCreate={sendPoll}
      />

      {/* EVENT */}
      <EventComposerSheet
        visible={eventSheetOpen}
        onClose={() => setEventSheetOpen(false)}
        onCreate={sendEvent}
      />
    </>
  );
}
