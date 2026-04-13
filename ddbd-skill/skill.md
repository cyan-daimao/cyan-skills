# DDBD 架构规范（后端代码生成必须遵守）

## 铁律
1. 禁止贫血模型，领域对象必须充血
2. Controller 不调 Mapper，不跳过 Service 直接查库
3. 只在 Controller 层用 UserHolder 获取用户信息，透传给其他层
4. 所有查询必须经过领域层
5. 每个接口有独立的 DTO/Request 对象，禁止复用
6. 禁止用 DO/Domain 直接返回前端，必须转 DTO

## 四层职责

| 层 | 职责 | 返回 |
|---|------|------|
| **Adapter** | HTTP/RPC 入口，参数接收 + 结果返回 | DTO |
| **Application** | 流程编排、业务组装、业务校验 | BO |
| **Domain** | 充血模型：实体 + 行为 + 规则，定义仓储接口和 Query | Domain |
| **Infrastructure** | 数据存取、仓储实现、雪花ID生成、第三方调用 | DO |

## 对象规范

| 对象 | 定义层 | 用途 | 关键规则 |
|------|--------|------|---------|
| DO | Infra | 与数据库表一一对应 | 字段类型与数据库列完全一致；枚举可直接映射 varchar |
| Domain | Domain | 封装属性 + 行为 | id 用 String；充血模型，不写领域服务 |
| BO | App | 多领域聚合 | 由 Application 层构建 |
| DTO | Adapter | 返回给前端/RPC | 禁止暴露内部结构 |
| Cmd | App | 写操作入参 | — |
| Query | Domain | 查询入参 | — |

## 转换链路（不可跳过、不可逆序）

```
读：DO → Domain → BO → DTO
写：Cmd → Domain → (仓储) → DO
查：Query 透传 Adapter → App → Domain → Infra
```

## 关键约定

**ID 字段**：Domain 层 String，DO 层与数据库一致（bigint→Long）。原因：前端 JS 对 2^53 以上的 Long 精度丢失。

**枚举**：DO 中可直接声明枚举属性映射数据库 varchar，无需字符串转换。枚举格式：`ENUM_NAME("code","描述")`

**断言**：`Assert.notBlank(name, new BizException("msg"))` — 不满足立即抛异常。

**类型转换**：DO ↔ Domain 推荐用 MapStruct 等映射工具自动生成。

**组装器 Assembler**：BO 按需组装，不同接口只组装需要的数据，避免全量加载。

**响应格式**：`Response<T> { code, message, data }`

**分页**：`PageQuery { pageNum, pageSize }` → `PageResult<T> { list, total, pageNum, pageSize }`

## 代码模板

### Adapter
```
Controller {
    GET /xx/query (XxxQuery query) → Response<XxxDTO> {
        XxxBO bo = XxxService.queryOne(query)
        return Response.success(XxxConvert.toDTO(bo))
    }
    POST /xx (XxxCmd cmd) → Response<XxxDTO> {
        XxxBO bo = XxxService.save(cmd)
        return Response.success(XxxConvert.toDTO(bo))
    }
    PUT /xx/{id} (String id, XxxCmd cmd) → Response<XxxDTO> {
        XxxBO bo = XxxService.update(id, cmd)
        return Response.success(XxxConvert.toDTO(bo))
    }
    DELETE /xx/{id} (String id) → Response<XxxDTO> {
        XxxBO bo = XxxService.delete(id)
        return Response.success(XxxConvert.toDTO(bo))
    }
    GET /xx/page (XxxQuery query) → Response<PageResult<XxxDTO>> {
        PageResult<XxxBO> page = XxxService.page(query)
        return Response.success(XxxConvert.toPageDTO(page))
    }
}
```

### Application
```
XxxService {
    queryOne(XxxQuery query) → XxxBO {
        Xxx domain = XxxRepository.queryOne(query)
        Assert.notNull(domain, new BizException("不存在"))
        XxxBO bo = XxxConvert.toBO(domain)
        XxxBOAssembler.assembleXxx(bo)   // 按需组装
        return bo
    }
    save(XxxCmd cmd) → XxxBO {
        Xxx domain = XxxConvert.toDomain(cmd)
        domain.save(XxxRepository, ...)   // 充血模型，业务逻辑在领域对象内
        return queryOne(XxxQuery{id: domain.id})
    }
}
```

### Domain（充血模型）
```
Xxx {
    String id
    ...

    save(XxxRepository repo, ...) {
        Assert.isBlank(id, new BizException("id必须为空"))    // 新增时 id 为空
        // 业务校验...
        repo.save(this)
    }

    delete(XxxRepository repo) {
        Assert.notBlank(id, new BizException("id不能为空"))
        repo.delete(this)
    }
}
```

### Infrastructure
```
XxxRepositoryImpl implements XxxRepository {
    queryOne(XxxQuery query) → Xxx {
        XxxDO xo = XxxMapper.selectById(toLong(query.id))
        return XxxConvert.toDomain(xo)       // Long id → String id
    }
    save(Xxx domain) {
        domain.id = String.valueOf(snowflakeId.nextId())
        XxxDO xo = XxxConvert.toDO(domain)   // String id → Long id
        XxxMapper.insert(xo)
    }
}
```

### 一对多模式（Order + OrderItem）
```
// Domain
Order {
    String id;  List<OrderItem> items
    addItem(OrderItem item) {
        Assert.notBlank(item.productId, new BizException("商品id不能为空"))
        items.add(item)
    }
    totalPrice() → Decimal {
        return items.sum(it → it.price * it.quantity)
    }
}
// Infra：主表 + 子表一起保存
OrderRepositoryImpl.save(Order order) {
    order.id = snowflakeId()
    OrderMapper.insert(toOrderDO(order))
    order.items.forEach(item → {
        item.id = snowflakeId()
        item.orderId = order.id
        OrderItemMapper.insert(toOrderItemDO(item))
    })
}
```

### 状态机模式
```
Order {
    OrderStatus status   // 枚举：CREATED / PAID / SHIPPED / COMPLETED / CANCELLED
    pay(PaymentRepository repo) {
        Assert.isTrue(status == CREATED, new BizException("只有待支付可支付"))
        status = PAID
        repo.recordPayment(this)
    }
    ship() {
        Assert.isTrue(status == PAID, new BizException("只有已支付可发货"))
        status = SHIPPED
    }
}
```

### 组装器模式
```
XxxBOAssembler {
    assembleAaa(XxxBO bo) { bo.aaa = AaaRepository.findById(bo.aaaId) }
    assembleBbb(XxxBO bo) { bo.bbb = BbbRepository.findById(bo.bbbId) }
    assembleAll(XxxBO bo) { assembleAaa(bo); assembleBbb(bo) }
}

// Application 按场景选择
list(query)    → 直接返回，不组装
detail(id)     → assembleAll(bo)
queryXxx(id)   → assembleXxx(bo)   // 只组装需要的
```
