export default defineBackground(() => {
  // Clicking the toolbar icon opens the side panel directly - no popup step.
  browser.sidePanel.setPanelBehavior({ openPanelOnActionClick: true }).catch((error: unknown) => {
    console.error('Failed to set side panel behavior', error);
  });
});
