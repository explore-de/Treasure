-- Personal API tokens for the MCP endpoint (/mcp). Only the SHA-256 hash of a
-- token is stored; the token itself is shown once when it is created. roles is
-- a comma-separated copy of the owner's roles at creation time, because roles
-- come from the OIDC login and are not stored anywhere else.

create sequence McpToken_SEQ start with 1 increment by 50;

create table McpToken (
    id bigint not null,
    member_id bigint not null,
    name varchar(100) not null,
    tokenHash varchar(64) not null unique,
    tokenPrefix varchar(16) not null,
    roles varchar(255),
    createdAt timestamp(6) not null,
    lastUsedAt timestamp(6),
    primary key (id)
);

alter table if exists McpToken add constraint FK_mcptoken_member
    foreign key (member_id) references Member on delete cascade;
