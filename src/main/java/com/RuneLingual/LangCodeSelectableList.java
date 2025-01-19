package com.RuneLingual;

import lombok.AccessLevel;
import lombok.Getter;

import javax.inject.Inject;

@Getter
public enum LangCodeSelectableList
{
    ENGLISH ("en", "EN","EN", 8, 6, false, false, false, false, true),
    //PORTUGUÊS_BRASILEIRO ("pt_br", "PT","PT-BR", 8, 6, false, false, false, false, true),
    //NORSK("no", "NB", "NB", 8, 6, false, false, false, false, true),
    日本語("ja", "JA", "JA", 15, 13, true, true, true, true, false);
    // todo: add languages here

    private final String langCode;
    private final String deeplLangCodeSource;
    private final String deeplLangCodeTarget;
    private final int charWidth;
    private final int chatBoxCharWidth;

    @Getter(AccessLevel.NONE)
    private final boolean needCharImages;
    @Getter(AccessLevel.NONE)
    private final boolean swapMenuOptionAndTarget;
    @Getter(AccessLevel.NONE)
    private final boolean needInputOverlay;
    @Getter(AccessLevel.NONE)
    private final boolean needInputCandidateOverlay;
    @Getter(AccessLevel.NONE)
    private final boolean needSpaceBetweenWords;

    @Inject
    LangCodeSelectableList(String langCode, String deeplCodeSrc, String deeplCodeTgt, int charWidth, int chatBoxCharWidth,
                           boolean needCharImages, boolean swapMenuOptionAndTarget,
                           boolean needInputOverlay, boolean needInputCandidateOverlay,
                           boolean needSpaceBetweenWords){
        this.langCode = langCode;
        this.deeplLangCodeSource = deeplCodeSrc;
        this.deeplLangCodeTarget = deeplCodeTgt;
        this.charWidth = charWidth;
        this.chatBoxCharWidth = chatBoxCharWidth;
        this.needCharImages = needCharImages;
        this.swapMenuOptionAndTarget = swapMenuOptionAndTarget;
        this.needInputOverlay = needInputOverlay;
        this.needInputCandidateOverlay = needInputCandidateOverlay;
        this.needSpaceBetweenWords = needSpaceBetweenWords;
    }

    public boolean needsCharImages() {
        return needCharImages;
    }
    public boolean needsSwapMenuOptionAndTarget() {
        return swapMenuOptionAndTarget;
    }

    public boolean needsInputOverlay() {
        return needInputOverlay;
    }

    public boolean needsInputCandidateOverlay() {
        return needInputCandidateOverlay;
    }

    public boolean needsSpaceBetweenWords() {
        return needSpaceBetweenWords;
    }

}
