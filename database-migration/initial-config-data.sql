-- ============================================================================
-- LiteFlow 初始配置数据
-- 从静态 XML 配置转换而来
-- ============================================================================

-- ----------------------------------------------------------------------------
-- flow_chain 表数据
-- ----------------------------------------------------------------------------

-- 订单处理流程
INSERT INTO flow_chain (
    chain_name,
    chain_desc,
    chain_code,
    application_name,
    chain_enable,
    namespace,
    tenant_id,
    status,
    create_time,
    update_time
) VALUES (
    'orderProcessChain',
    '标准订单处理流程，包括验证、库存检查、金额计算和创建订单',
    'THEN(validateOrder, checkStock, createOrder)',
    'dms-liteflow',
    1,
    'default',
    1,
    'PUBLISHED',
    NOW(),
    NOW()
);

-- 订单审批流程
INSERT INTO flow_chain (
    chain_name,
    chain_desc,
    chain_code,
    application_name,
    chain_enable,
    namespace,
    tenant_id,
    status,
    create_time,
    update_time
) VALUES (
    'orderApprovalChain',
    '带VIP判断的订单审批流程',
    'THEN(validateOrder, WHEN(checkStock, calculateAmount), IF(isVIPUser, vipApproval, normalApproval), createOrder)',
    'dms-liteflow',
    1,
    'default',
    1,
    'PUBLISHED',
    NOW(),
    NOW()
);

-- 通知处理子流程
INSERT INTO flow_chain (
    chain_name,
    chain_desc,
    chain_code,
    application_name,
    chain_enable,
    namespace,
    tenant_id,
    status,
    create_time,
    update_time
) VALUES (
    'notifyProcess',
    '通知处理子流程，可被其他流程引用',
    'THEN(sendEmail, sendSMS)',
    'dms-liteflow',
    1,
    'default',
    1,
    'PUBLISHED',
    NOW(),
    NOW()
);

-- ----------------------------------------------------------------------------
-- rule_component 表数据（组件定义）
-- 注意：组件是 Java 类，不是脚本，所以 language = 'java'
-- ----------------------------------------------------------------------------

-- 业务组件
INSERT INTO rule_component (component_id, component_name, component_code, component_type, language, application_name, script_enable, tenant_id, status, create_time, update_time)
VALUES
('validateOrder', '订单验证', 'ValidateOrderComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('checkStock', '库存检查', 'CheckStockComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('calculateAmount', '金额计算', 'CalculateAmountComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('createOrder', '创建订单', 'CreateOrderComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('vipApproval', 'VIP审批', 'VipApprovalComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('normalApproval', '普通审批', 'NormalApprovalComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('sendEmail', '发送邮件', 'SendEmailComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW()),
('sendSMS', '发送短信', 'SendSMSComponent', 'component', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW());

-- 条件组件
INSERT INTO rule_component (component_id, component_name, component_code, component_type, language, application_name, script_enable, tenant_id, status, create_time, update_time)
VALUES
('isVIPUser', '是否VIP用户', 'IsVIPUserComponent', 'if_script', 'java', 'dms-liteflow', 1, 1, 'PUBLISHED', NOW(), NOW());
