<script setup lang="ts">
import DetailScreen from '@terra/mobile/screens/Detail.vue'
import { ref } from 'vue'
import { onLoad, onShareAppMessage } from '@dcloudio/uni-app'
const foodId = ref(0)
onLoad(query => { foodId.value = Number(query?.id) || 0 })
onShareAppMessage(() => ({ title: '大炎珍馐志', path: '/pages/detail/index?id=' + foodId.value }))
</script>
<template><DetailScreen :food-id="foodId" /></template>
