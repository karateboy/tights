<template>
    <div>
        <form class="form-horizontal" @submit.prevent="endDye">
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
    import axios from 'axios'

    export default{
        data(){
            return {
                id: "",
                invalidId: false
            }
        },
        methods: {
            endDye(){

                axios.get("/EndDye/"+this.id).then((resp)=>{
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
            }
        }
    }
</script>
