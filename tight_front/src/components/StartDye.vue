<template>
    <div>
        <form class="form-horizontal" @submit.prevent="startDye">
            <div class="form-group" >
                <label class="col-lg-3 control-label">染色人員:</label>
                <div class="btn-group" data-toggle="buttons">
                    <label class="btn btn-outline btn-primary"
                           v-for="person in operatorList"
                           @click="operator=person"
                           :class="{active: operator==person }">
                        <input type="radio">{{ person }} </label>
                </div>
            </div>
            <div class="form-group has-feedback"><label class="col-lg-3 control-label">漂染單編號:</label>
                <div class="col-lg-5"><input type="text" placeholder="掃描條碼" autofocus
                                             class="form-control"
                                             v-model="id">
                    <span v-if="invalidId" class="help-block">無效的漂染包襪明細單號</span>
                </div>
            </div>
        </form>
    </div>
</template>
<style scoped>
    body{
        background-color:#ff0000;
    }


</style>
<script>
    import DyeCardDetail from './DyeCardDetail.vue'
    import axios from 'axios'

    export default{
        data(){
            return {
                id: "",
                invalidId: false,
                operatorList: ['康志明', '劉守任', '詹鎮岳', '陳炳翔', '許君豪'],
                operator:'康志明'
            }
        },
        methods: {
            startDye(){
                const param  = {
                    _id:this.id,
                    operator:this.operator
                }

                axios.post("/StartDye", param).then((resp)=>{
                    if(resp.status == 200){
                        alert("成功")
                        this.cleanup()
                    }else{
                        alert("找不到漂染卡紀錄:" + resp.statusText)
                        this.id = ""
                    }
                }).catch((err)=>{
                    alert(err)
                })
            },
            cleanup(){
                this.id = ""
                this.operator = this.operatorList[0]
            }
        },
        components: {
            DyeCardDetail
        }
    }
</script>
