<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { restoreSession } from '@terra/mobile/session'
onLaunch(() => { void restoreSession() })
</script>
<style>
@import "@terra/mobile/styles.css";
</style>
