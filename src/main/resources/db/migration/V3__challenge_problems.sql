-- 도전 문제: 두 단계로 푸는 문제, 거꾸로 생각하는 문제, 함정 숫자가 섞인 문제.
--
-- second_operation  두 단계 문제의 두 번째 연산. 한 단계면 비어 있다.
-- operand_slots     식에 들어가는 수를 슬롯 이름으로 적은 것. "a,b" / "a,b,c" / "a,b,a"(같은 수를 두 번 씀).
--                   슬롯 중 여기 없는 것은 문장에만 나오는 함정 숫자다.
-- hard              기본 학습지에는 안 나오고 '도전' 을 켰을 때만 나온다.

alter table problem_template add column second_operation varchar(20);
alter table problem_template add column operand_slots varchar(20) not null default 'a,b';
alter table problem_template add column hard boolean not null default false;

alter table assignment add column challenge boolean not null default false;
