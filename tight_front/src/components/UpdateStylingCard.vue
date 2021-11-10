<template>
  <div>
    <form class="form-horizontal" @submit.prevent="query">
      <div class="form-group has-feedback">
        <label class="col-lg-3 control-label">流動工作卡號:</label>
        <div class="col-lg-5">
          <input
            type="text"
            placeholder="掃描條碼"
            autofocus
            class="form-control"
            v-model="workCardID"
          />
        </div>
      </div>
    </form>
    <br />
    <div v-if="displayCard">
      <table class="table table-bordered table-condensed">
        <thead>
          <tr class="info">
            <th>訂單編號</th>
            <th class="text-center">品項</th>
            <th class="text-center">顏色</th>
            <th class="text-center">尺寸</th>
            <th class="text-center">工廠代號</th>
            <th class="text-center">客戶編號</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{{ workCardOrderID }}</td>
            <td>{{ workCardName }}</td>
            <td>{{ wordCardColor }}</td>
            <td>{{ workCardSize }}</td>
            <td>{{ factoryId }}</td>
            <td>{{ customerId }}</td>
          </tr>
        </tbody>
      </table>
      <work-card-detail v-if="workCard" :workCard="workCard" />
      <styling-card
        :stylingCard="stylingCard"
        :workCardID="workCardID"
        :quantity="quantity"
        @updated="cleanup"
      ></styling-card>
    </div>
  </div>
</template>
<style scoped>
body {
  background-color: #ff0000;
}
</style>
<script>
import axios from 'axios';
import StylingCard from './StylingCard.vue';
import WorkCardDetail from './WorkCardDetail.vue';
import cardHelper from '../cardHelper';

export default {
  data() {
    return {
      workCardID: '',
      displayCard: false,
      workCard: {
        order: {},
      },
      stylingCard: undefined,
      quantity: 0,
    };
  },
  computed: {
    workCardOrderID() {
      if (this.workCard && this.workCard.orderId) return this.workCard.orderId;
      else return '查詢中';
    },
    workCardName() {
      if (this.workCard && this.workCard.order && this.workCard.order.name) {
        return this.workCard.order.name;
      } else return '查詢中';
    },
    wordCardColor() {
      if (this.workCard && this.workCard.order && this.workCard.order.details) {
        return this.workCard.order.details[this.workCard.detailIndex].color;
      } else return '查詢中';
    },
    workCardSize() {
      if (this.workCard && this.workCard.order && this.workCard.order.details) {
        return this.workCard.order.details[this.workCard.detailIndex].size;
      } else return '查詢中';
    },
    factoryId() {
      if (
        this.workCard &&
        this.workCard.order &&
        this.workCard.order.factoryId
      ) {
        return this.workCard.order.factoryId;
      } else return '查詢中';
    },
    customerId() {
      if (
        this.workCard &&
        this.workCard.order &&
        this.workCard.order.customerId
      ) {
        return this.workCard.order.customerId;
      } else return '查詢中';
    },
  },
  methods: {
    query() {
      axios
        .get('/WorkCard/' + this.workCardID)
        .then(resp => {
          const ret = resp.data;
          if (resp.status == 200) {
            let workCard = ret;
            this.workCard = Object.assign(workCard, this.workCard);
            if (workCard.stylingCard === null)
              this.stylingCard = {
                operator: [],
                date: 0,
              };
            else this.stylingCard = workCard.stylingCard;
            this.displayCard = true;
            this.quantity = workCard.quantity;
            cardHelper.populateWorkCard(this.workCard);
          } else {
            alert('找不到流動工作卡:');
            this.workCardID = '';
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    cleanup() {
      this.displayCard = false;
      this.workCardID = '';
    },
  },
  components: {
    WorkCardDetail,
    StylingCard,
  },
};
</script>
