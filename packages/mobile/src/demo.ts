import type { Food, Region } from './types'
export const demoRegions: Region[] = [
  { id: 1, name: '成都', province: '四川' }, { id: 2, name: '杭州', province: '浙江' }, { id: 3, name: '广州', province: '广东' },
]
export const demoFoods: Food[] = [
  { id: 1, name: '一碗担担面', region: demoRegions[0]!, summary: '红油的热烈，藏着街巷里的日常。', story: '这是一份用于浏览界面演示的示例食记。接入服务器后，这里将展示真实菜品的故事。', ingredients: '面条、芽菜、花生、红油', heat: 128 },
  { id: 2, name: '西湖醋鱼', region: demoRegions[1]!, summary: '湖山入味，酸甜之间是江南。', story: '这是一份示例食记，展示菜品详情的排版。', ingredients: '鲜鱼、香醋、姜', heat: 86 },
  { id: 3, name: '一盅两件', region: demoRegions[2]!, summary: '把早晨留给一壶茶和慢下来的时光。', story: '这是一份示例食记，展示移动端的浏览流程。', ingredients: '茶、虾饺、烧卖', heat: 64 },
]
